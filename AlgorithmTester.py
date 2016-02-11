
from random import random
import numpy as np
import scikits.bootstrap as bootstrap

class AlgorithmTester:
    def __init__(self, algorithm, tester):
        self.algorithm_id = algorithm["id"]
        self.algorithm_name = algorithm["algorithm"]
        self.tester = tester
        self.algorithm_results = tester.results_by_algorithm[self.algorithm_id]
        self.get_best_threshold_for_algorithm()

    def get_best_threshold_for_algorithm(self):
        X = np.arange(.1,.4,0.0001)
        best_threshold = 0
        best_f1 = 0
        Y = []
        for threshold in X:
            # don't leave out any articles or queries by using None
            f1_measure = self.f1(None, None, self.algorithm_id, threshold)
            if f1_measure > best_f1:
                best_f1 = f1_measure
                best_threshold = threshold
            Y.append(f1_measure)
        self.best_threshold = best_threshold

        #self.plot_threshold_and_results_multi_algorithm(X_vals, labels, Y_vals, x_label, y_label, title)
        x_label = "Threshold"
        y_label = "F1 Measure"
        title = "F1 Measure by Threshold for {} Validator".format(self.algorithm_name)
        self.tester.plot_threshold_and_results_multi_algorithm([X], None, [Y], x_label, y_label, title)
        return best_threshold

    def test(self, random=False):
        #X = np.arange(self.best_threshold - .05, self.best_threshold + .05, .005)
        X = np.arange(.1,.4,0.005)
        best_f1_measures = []
        best_thresholds = []
        true_positives = 0
        false_positives = 0
        false_negatives = 0
        for article in self.tester.article_ids:
            for query in self.tester.query_ids:
                f1_measures = []
                best_threshold = 0
                best_f1 = 0
                for threshold in X:
                    f1_measure = self.f1(article, query, self.algorithm_id, threshold)
                    f1_measures.append(f1_measure)
                    if f1_measure > best_f1:
                        best_f1 = f1_measure
                        best_threshold = threshold
                best_f1_measures.append(best_f1)
                best_thresholds.append(best_threshold)
                t_p, f_p, f_n = self.validate_query_article_left_out(article, query, self.best_threshold)

                if random:
                    t_p, f_p, f_n = self.validate_random(article, query)

                true_positives += t_p
                false_positives += f_p
                false_negatives += f_n
        f1 = self.calculate_f1(true_positives, false_positives, false_negatives)
        print(self.algorithm_name, f1)
        return best_thresholds, best_f1_measures, f1


    def validate_random(self, article, query):
        actual_value = self.tester.query_articles[(query, article)]
        test_value = random()

        random_threshold = self.tester.dataSource.get_validation_ration()
        test_value = (test_value < random_threshold)

        true_positives, false_positives, false_negatives = 0, 0, 0
        if test_value and actual_value:
            true_positives = 1
        elif test_value and not actual_value:
            false_positives = 1
        elif not test_value and actual_value:
            false_negatives = 1

        return true_positives, false_positives, false_negatives


    def f1_bootstrap(self, dataset):
        true_positives = 0
        false_positives = 0
        false_negatives = 0
        for datum in dataset:
            query_id = datum[0][0]
            article_id = datum[0][1]
            test_value_probability = datum[1]
            actual_value = self.tester.query_articles[(query_id, article_id)]

            test_value = (test_value_probability > self.best_threshold)
            if test_value and actual_value:
                true_positives += 1
            elif test_value and not actual_value:
                false_positives += 1
            elif not test_value and actual_value:
                false_negatives += 1
        return self.calculate_f1(true_positives, false_positives, false_negatives)


    def bootstrap(self):
        data = list(self.algorithm_results.items())
        CIs = bootstrap.ci(data=data, statfunction=self.f1_bootstrap, n_samples=100)
        print("Bootstrapped 95% confidence intervals for f1 \nLow:", CIs[0], "\nHigh:", CIs[1])


    def validate_query_article_left_out(self, article_left_out, query_left_out, threshold):
        test_value_probability = self.algorithm_results[(query_left_out, article_left_out)]
        actual_value = self.tester.query_articles[(query_left_out, article_left_out)]
        test_value = (test_value_probability > threshold)
        true_positives, false_positives, false_negatives = 0, 0, 0
        if test_value and actual_value:
            true_positives = 1
        elif test_value and not actual_value:
            false_positives = 1
        elif not test_value and actual_value:
            false_negatives = 1

        return true_positives, false_positives, false_negatives

    #I know this is inneficient we can fix it later
    def f1(self, article_left_out, query_left_out, algorithm_id, threshold):
        true_positives = 0
        false_positives = 0
        false_negatives = 0
        for article_id in self.tester.article_ids:
            for query_id in self.tester.query_ids:
                if article_id != article_left_out or query_id != query_left_out:
                    test_value_probability = self.algorithm_results[(query_id, article_id)]
                    actual_value = self.tester.query_articles[(query_id, article_id)]
                    test_value = (test_value_probability > threshold)
                    if test_value and actual_value:
                        true_positives += 1
                    elif test_value and not actual_value:
                        false_positives += 1
                    elif not test_value and actual_value:
                        false_negatives += 1
        f1 = self.calculate_f1(true_positives, false_positives, false_negatives)

        return f1

    def calculate_f1(self, true_positives, false_positives, false_negatives):
        recall = self.recall(true_positives, false_negatives)
        precision = self.precision(true_positives, false_positives)

        if recall + precision == 0:
            return 0
        f1 = 2 * (precision * recall)/(precision + recall)

        return f1


    def precision(self, true_positives, false_positives):
        if true_positives + false_positives == 0:
            return 1
        return true_positives/(true_positives + false_positives)

    def recall(self, true_positives, false_negatives):
        if true_positives + false_negatives == 0:
            return 1
        return true_positives/(true_positives + false_negatives)
