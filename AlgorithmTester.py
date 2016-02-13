import random
import numpy as np
import scikits.bootstrap as bootstrap
from Tester import *
from collections import Counter, defaultdict
import matplotlib.pyplot as plt

class AlgorithmTester:
    def __init__(self, algorithm_id, algorithm_name, tester_datasource=TesterDataSource()):
        """
        Creates a test environment for a given algorithm
        :param algorithm_id id of algorithm of interest
        :param algorithm_name name of algorithm of interest
        :param tester_datasource: tester datasouce instance to help us get data, default new
        :return:
        """
        self.algorithm_id = algorithm_id
        self.algorithm_name = algorithm_name
        self.dataSource = tester_datasource
        self.query_articles = self.dataSource.get_query_articles()
        self.results = self.dataSource.get_validation_results()
        self.article_ids = self.dataSource.get_articles()
        self.query_ids = self.dataSource.get_queries()
        self.algorithm_results = tester_datasource.get_results_by_algorithms(self.algorithm_id)
        self.get_best_threshold_for_algorithm()

        #controls for variation in "randomness"
        random.seed(10)

    def get_best_threshold_for_algorithm(self):
        """
        finds best threshold value for whether or not a query validates an algorithm
        :return:
        """
        X = np.arange(.1,.4,0.0001)
        best_threshold = 0
        best_f1 = 0
        Y = []
        for threshold in X:
            # don't leave out any articles or queries by using None
            f1_measure = self.f1(None, None, threshold)
            if f1_measure > best_f1:
                best_f1 = f1_measure
                best_threshold = threshold
            Y.append(f1_measure)
        self.best_threshold = best_threshold

        #self.plot_threshold_and_results_multi_algorithm(X_vals, labels, Y_vals, x_label, y_label, title)
        x_label = "Threshold"
        y_label = "F1 Measure"
        title = "F1 Measure by Threshold for {} Validator".format(self.algorithm_name)
        return best_threshold

    def test(self, distribution_algorithm=False, output_full_results=False):
        X = np.arange(self.best_threshold - .05, self.best_threshold + .05, .005)
        #X = np.arange(.1,.4,0.005)
        best_f1_measures = []
        best_thresholds = []
        results = defaultdict(list)
        for article in self.article_ids:
            for query in self.query_ids:
                if distribution_algorithm:
                    self.validate_distribution_algorithm(article, query, distribution_algorithm, results)
                else:
                    f1_measures = []
                    best_threshold = 0
                    best_f1 = 0
                    for threshold in X:
                        f1_measure = self.f1(article, query, threshold)
                        f1_measures.append(f1_measure)
                        if f1_measure > best_f1:
                            best_f1 = f1_measure
                            best_threshold = threshold
                    best_f1_measures.append(best_f1)
                    best_thresholds.append(best_threshold)
                    self.validate_query_article_left_out(article, query, self.best_threshold, results)

        true_positives = len(results["true_positives"])
        false_positives = len(results["false_positives"])
        false_negatives = len(results["false_negatives"])
        f1 = self.calculate_f1(true_positives, false_positives, false_negatives)
        print(self.algorithm_name, f1)
        if output_full_results:
            self.output_results(results)
        return best_thresholds, best_f1_measures, f1


    def validate_distribution_algorithm(self, article, query, distribution_algorithm, results):
        """
        Randomly decides whether a query validates an algorithm
        :param article: article_id to validate
        :param query: query_id to validate
        :param distribution_algorithm: the identifier of the random algorithm, possible values are
            all_true, all_false, half_and_half, real_distribution
        :return:
        """
        actual_value = self.query_articles[(query, article)]

        if distribution_algorithm == "all_true":
            test_value = True

        elif distribution_algorithm == "all_false":
            test_value = False

        elif distribution_algorithm == "half_and_half":
            test_value = random.randint(0, 1)

        elif distribution_algorithm == "real_distribution":
            # weights with the same distribution as
            test_value = random.random()

            random_threshold = self.dataSource.get_validation_ratio()
            test_value = (test_value < random_threshold)

        if test_value and actual_value:
            results["true_positives"].append((query, article))
        elif test_value and not actual_value:
            results["false_positives"].append((query, article))
        elif not test_value and actual_value:
            results["false_negatives"].append((query, article))


    def f1_bootstrap(self, dataset):
        true_positives = 0
        false_positives = 0
        false_negatives = 0
        for datum in dataset:
            query_id = datum[0][0]
            article_id = datum[0][1]
            test_value_probability = datum[1]
            actual_value = self.query_articles[(query_id, article_id)]

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


    def f1_randomized(self, test_values, actual_values):
        random.shuffle(actual_values)
        true_positives = 0
        false_positives = 0
        false_negatives = 0
        for i in range(len(test_values)):
            test_value = test_values[i]
            actual_value = actual_values[i]
            if test_value and actual_value:
                true_positives += 1
            elif test_value and not actual_value:
                false_positives += 1
            elif not test_value and actual_value:
                false_negatives += 1
        return self.calculate_f1(true_positives, false_positives, false_negatives)

    def create_randomization_distribution_f1(self):
        test_values = []
        actual_values = []
        f1s = Counter()
        f1s_array =[]
        for query in self.query_ids:
            for article in self.article_ids:
                test_value_probability = self.algorithm_results[(query, article)]
                test_value = (test_value_probability > self.best_threshold)
                actual_value = self.query_articles[(query, article)]
                test_values.append(test_value)
                actual_values.append(actual_value)
        for i in range(10000):
            # f1_bucket = round(self.f1_randomized(test_values, actual_values), 2)
            # f1s[f1_bucket] += 1
            f1s_array.append(self.f1_randomized(test_values, actual_values))
        return f1s_array

    def graph_randomization_distribution_f1(self):
        f1s = self.create_randomization_distribution_f1()

        # the histogram of the data
        n, bins, patches = plt.hist(f1s, 50, normed=1, facecolor='green', alpha=0.75)
        plt.grid(True)

        plt.show()


    def calculate_p_value(self):
        f1s = self.create_randomization_distribution_f1()
        _, _, f1 = self.test()
        as_extreme = 0
        for f1_random in f1s:
            if f1_random >= f1:
                as_extreme += 1
        p_value = as_extreme/len(f1s)
        return p_value







    def validate_query_article_left_out(self, article_left_out, query_left_out, threshold, results):
        """
        Checks if the stored validates result of a query-article pair is equal to the result from the algorithm
        with the given threshold and add it as a true positive, false positive, or false negative to the results
        :param article_left_out: the article to validate
        :param query_left_out: the query to validate
        :param threshold: the threshold to be considered a match
        :param results: the dictionary to place results in
        :return: None
        """
        # get results from validation_algorithms table
        test_value_probability = self.algorithm_results[(query_left_out, article_left_out)]
        # get results from query_articles table
        actual_value = self.query_articles[(query_left_out, article_left_out)]
        # check if we pass threshold
        test_value = (test_value_probability > threshold)
        if test_value and actual_value:
            results["true_positives"].append((query_left_out, article_left_out))
        elif test_value and not actual_value:
            results["false_positives"].append((query_left_out, article_left_out))
        elif not test_value and actual_value:
            results["false_negatives"].append((query_left_out, article_left_out))

    def f1(self, article_left_out, query_left_out, threshold):
        """
        Checks over all queries and articles that are not being left out if the stored validates result of
        that query-article pair is equal to the result from the algorithm with the given threshold
        Sums up true positives, false positives, and false negatives, and calculates the f1 measure
        :param article_left_out: the article to leave out
        :param query_left_out: the query to leave out
        :param threshold: the threshold to be considered a match
        :return: the f1 score
        """
        true_positives = 0
        false_positives = 0
        false_negatives = 0
        for article_id in self.article_ids:
            for query_id in self.query_ids:
                # only look at query-article pairs that are not related to those being left our
                if article_id != article_left_out or query_id != query_left_out:
                    test_value_probability = self.algorithm_results[(query_id, article_id)]
                    actual_value = self.query_articles[(query_id, article_id)]
                    # check if the value given by the algorithm passes the threshold
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
        """

        :param true_positives: the number of true positives in the data set
        :param false_positives: the number of false positives in the data set
        :param false_negatives: the number of false negatives in the data set
        :return: the f1 measure
        """
        recall = self.recall(true_positives, false_negatives)
        precision = self.precision(true_positives, false_positives)

        if recall + precision == 0:
            return 0
        f1 = 2 * (precision * recall)/(precision + recall)

        return f1

    @staticmethod
    def precision(true_positives, false_positives):
        """

        :param true_positives: the number of true positives in the data set
        :param false_positives: the number of false positives in the data set
        :return: the precision measure
        """
        if true_positives + false_positives == 0:
            return 1
        return true_positives/(true_positives + false_positives)

    @staticmethod
    def recall(true_positives, false_negatives):
        """

        :param true_positives: the number of true positives in the data set
        :param false_negatives: the number of false negatives in the data set
        :return: the recall measure
        """
        if true_positives + false_negatives == 0:
            return 1
        return true_positives/(true_positives + false_negatives)

    def output_results(self, results):
        """
        Outputs query-article pairs for the algorithm that result in true positives, false positives, and false negatives
        :param results: a dictionary of testing results
        :return: None
        """
        for key in results:
            print(key)
            for (query, article) in results[key]:
                print("{0} -- {1}".format(self.dataSource.get_query_as_string(query), self.dataSource.get_article_title(article)))


def main():
    at = AlgorithmTester(1, "keyword")
    at.calculate_p_value()
    # at.test("half_and_half")

if __name__ == "__main__":
    main()
