import random
import numpy as np
import scikits.bootstrap as bootstrap
from Tester import *

class AlgorithmTester:
    def __init__(self, algorithm, tester):
        self.algorithm_id = algorithm["id"]
        self.algorithm_name = algorithm["algorithm"]
        self.tester = tester
        self.algorithm_results = tester.results_by_algorithm[self.algorithm_id]
        self.get_best_threshold_for_algorithm()
        random.seed(10)

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

    def test(self, distribution_algorithm=False):
        # X = np.arange(self.best_threshold - .1, self.best_threshold + .1, .005)
        X = np.arange(.1,.4,0.005)
        best_f1_measures = []
        best_thresholds = []
        true_positives = 0
        false_positives = 0
        false_negatives = 0
        for article in self.tester.article_ids:
            for query in self.tester.query_ids:
                if distribution_algorithm:
                    t_p, f_p, f_n = self.validate_distribution_algorithm(article, query, distribution_algorithm)
                else:
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


                true_positives += t_p
                false_positives += f_p
                false_negatives += f_n
        f1 = self.calculate_f1(true_positives, false_positives, false_negatives)
        print(self.algorithm_name, f1)
        return best_thresholds, best_f1_measures, f1


    def validate_distribution_algorithm(self, article, query, distribution_algorithm):
        actual_value = self.tester.query_articles[(query, article)]

        if distribution_algorithm == "all_true":
            test_value = True

        elif distribution_algorithm == "all_false":
            test_value = False

        elif distribution_algorithm == "half_and_half":
            test_value = random.randint(0, 1)

        elif distribution_algorithm == "real_distribution":
            # weights with the same distribution as
            test_value = random.random()

            random_threshold = self.tester.dataSource.get_validation_ratio()
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
        CIs = bootstrap.ci(data=data, statfunction=self.f1_bootstrap, n_samples=10000)
        print(self.algorithm_name)
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

def main():
    at = AlgorithmTester({"id": 2, "algorithm": "swoogle"}, Tester())
    at.test("half_and_half")

if __name__ == "__main__":
    main()
