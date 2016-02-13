import random
import numpy as np
import scikits.bootstrap as bootstrap
from Tester import *
from collections import  Counter
import matplotlib.pyplot as plt
from TestDataSource import  *

class AlgorithmTester:
    def __init__(self, algorithm_id, algorithm_name, tester_datasource=TestDataSource()):
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
        :return: the best threshold value
        """
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
        return best_threshold

    def test(self, distribution_algorithm=False):
        """
        Performs leave-one-out cross-validation testing on the data
        :param distribution_algorithm: type of "random" distribution to validate with, possible values include
            all_true, all_false, half_and_half, real_distribution,
            DEFAULT False uses actual algorithm results (non-random)
        :return: The f1 measure of the results
        """
        X = np.arange(self.best_threshold - .05, self.best_threshold + .05, .005)
        #X = np.arange(.1,.4,0.005)
        best_f1_measures = []
        best_thresholds = []
        true_positives = 0
        false_positives = 0
        false_negatives = 0
        for article in self.article_ids:
            for query in self.query_ids:
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


    def validate_distribution_algorithm(self, article, query, distribution_algorithm="real_distribution"):
        """
        Randomly decides whether a query validates an algorithm
        :param article: article_id to validate
        :param query: query_id to validate
        :param distribution_algorithm: the identifier of the random algorithm, possible values are
            all_true, all_false, half_and_half, real_distribution
        :return: number of true positives, number of false positives, number of false negatives
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

        true_positives, false_positives, false_negatives = 0, 0, 0
        if test_value and actual_value:
            true_positives = 1
        elif test_value and not actual_value:
            false_positives = 1
        elif not test_value and actual_value:
            false_negatives = 1

        return true_positives, false_positives, false_negatives


    def f1_bootstrap(self, dataset):
        """
        bootstraps on f1 measure of a dataset of queries, articles, and validation measures
        :param dataset: a list of tuples: [((query_id, article_id), validation measure)...]
        :return: the f1 measure of the results
        """
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
        """
        performs bootrapping of f1 measure on dataset. A narrow confidence interval is more indicative of a sufficient sample size
        A 95% confidence interval means we are 95% confident that the true f1 measure is between (1) and (2).
        ( 1 and 2 are values return by bootstrap library).
        :return:
        """
        data = list(self.algorithm_results.items())
        CIs = bootstrap.ci(data=data, statfunction=self.f1_bootstrap, n_samples=10000)
        print(self.algorithm_name)
        print("Bootstrapped 95% confidence intervals for f1 \nLow:", CIs[0], "\nHigh:", CIs[1])


    def f1_randomized(self, test_values, actual_values):
        """
        Generates the f1 measure a dataset, except randomized. The actual validation values are shuffled
        and randomly paired with a test value before the f1 measure is calculated
        :param test_values: a list of booleans representing whether or not the algorithm decided if the query validated the article
        :param actual_values: a list of booleans representing whether or not the query actually validated the article
        :return: f1 measure
        """
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
        """
        creates a randomized distribution of f1 measures for hypothesis testing purposes
        :return: array of f1 measures from distribution
        """
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
            f1s_array.append(self.f1_randomized(test_values, actual_values))
        return f1s_array

    def graph_randomization_distribution_f1(self, randomization_distribution, test_value=None):
        """
        Graphs the randomization distribution of f1 values
        :return:
        """
        # the histogram of the data
        n, bins, patches = plt.hist(randomization_distribution, 50, normed=1, facecolor='green', alpha=0.75)
        plt.grid(True)
        if test_value != None:
            plt.axvline(x=test_value)
        plt.ylabel("Frequency")
        plt.xlabel("F1 measure")
        plt.title("Randomization Distribution of F1 Measures")
        legend = [mpatches.Patch(color="b", label="Test result")]
        plt.legend(handles=legend)
        plt.show()


    def calculate_p_value(self, randomization_distribution, test_value):
        """
        calculates p value of algorithm's results, assuming null hypothesis is true
        :return:
        """
        as_extreme = 0
        for f1_random in randomization_distribution:
            if f1_random >= test_value:
                as_extreme += 1
        p_value = as_extreme/len(randomization_distribution)
        return p_value


    def hypothesis_test(self):
        """
        Performs a statistical significance test on the data
        :return:
        """
        randomization_distribution = self.create_randomization_distribution_f1()
        _, _, test_value = self.test()
        p_value = self.calculate_p_value(randomization_distribution, test_value)
        h_0 = self.dataSource.get_validation_ratio()
        print("H_0: Population F1 = {}. {} performs as well as random.".format(h_0, self.algorithm_name))
        print("H_a: Population F1 > {}. {} performs better than random.".format(h_0, self.algorithm_name))
        if p_value < 0.01:
            print("A p value of {} is strong evidence to reject the null hypothesis that {} performs as well as random"\
            "in favor of the alternative that it performs better than random. Results are highly statistically significant.".format(p_value, self.algorithm_name))
        elif 0.01 <= p_value < 0.05:
            print("A p value of {} is evidence to reject the null hypothesis that {} performs as well as random"\
            "in favor of the alternative that it performs better than random. Results are statistically significant.".format(p_value, self.algorithm_name))
        elif 0.05 <= p_value < 0.1:
            print("A p value of {} is weak evidence to reject the null hypothesis that {} performs as well as random"\
                  "in favor of the alternative that it performs better than random. Results are marginally statistically significant.".format(p_value, self.algorithm_name))
        else:
            print("A p value of {} is not sufficient evidence to reject the null hypothesis that {} performs as well as random"\
                  "in favor of the alternative that it performs better than random. Results are not statistically significant.".format(p_value, self.algorithm_name))

        self.graph_randomization_distribution_f1(randomization_distribution, test_value=test_value)


    def validate_query_article_left_out(self, article_left_out, query_left_out, threshold):
        test_value_probability = self.algorithm_results[(query_left_out, article_left_out)]
        actual_value = self.query_articles[(query_left_out, article_left_out)]
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
        for article_id in self.article_ids:
            for query_id in self.query_ids:
                if article_id != article_left_out or query_id != query_left_out:
                    test_value_probability = self.algorithm_results[(query_id, article_id)]
                    actual_value = self.query_articles[(query_id, article_id)]
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
    at = AlgorithmTester(1, "keyword")
    at.hypothesis_test()
    # at.test("half_and_half")

if __name__ == "__main__":
    main()
