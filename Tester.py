from DataSource import *
import numpy as np
import scikits.bootstrap as bootstrap
import matplotlib.pyplot as plot
import matplotlib.patches as mpatches
from psycopg2.extras import RealDictCursor
from random import random
from collections import defaultdict
import json
import os

class AlgorithmTester:
    def __init__(self, algorithm, tester):
        self.algorithm_id = algorithm["id"]
        self.algorithm_name = algorithm["algorithm"]
        self.tester = tester
        self.algorithm_results = tester.results_by_algorithm[self.algorithm_id]
        self.calculate_best_threshold()

    #TODO FILL IN LAURAS METHOD
    def calculate_best_threshold(self):
        if self.algorithm_name == "Swoogle Semantic Analysis":
            self.best_threshold = 0.34
        else:
            self.best_threshold = 0.17

    def test(self, random=False):
        X = np.arange(.1,.4,0.025)
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

        #TODO: make this actual db value
        random_threshold = 91/3645
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
                if article_id != article_left_out and query_id != query_left_out:
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

class Tester:
    def __init__(self):
        self.dataSource = TesterDataSource()
        self.query_articles = self.dataSource.get_query_articles()
        self.results = self.dataSource.get_validation_results()
        self.article_ids = self.dataSource.get_articles()
        self.query_ids = self.dataSource.get_queries()
        self.algorithms = self.dataSource.get_algorithms()
        self.separate_algorithm_data()
        self.alg_testers = []
        for algorithm in self.algorithms:
            self.alg_testers.append(AlgorithmTester(algorithm, self))

    def test_all(self):
        labels = []
        Y_vals = []
        X_vals = []
        for alg_tester in self.alg_testers:
            X, Y, f1 = alg_tester.test()
            X_vals.append(X)
            Y_vals.append(Y)
            labels.append(alg_tester.algorithm_name)
        self.plot_threshold_and_results_multi_algorithm(X_vals, labels, Y_vals)

    def bootstrap_all(self):
        for alg_tester in self.alg_testers:
            alg_tester.bootstrap()




    def separate_algorithm_data(self):
        algorithm_datasets = defaultdict(dict)
        for algorithm in self.algorithms:
            algorithm_id = algorithm["id"]
            for query_id in self.query_ids:
                for article_id in self.article_ids:
                    algorithm_datasets[algorithm_id][(query_id, article_id)] = self.results[(query_id, article_id, algorithm_id)]
        self.results_by_algorithm = algorithm_datasets

    def plot_threshold_and_results_multi_algorithm(self, x_vals, labels, y_vals):
        colors = ["red", "blue", "yellow", "green", "orange", "purple", "pink"]
        color_index = 0
        key_legends = []
        for y_i, y in enumerate(y_vals):
            plot.scatter(x_vals[y_i], y, color=colors[color_index])
            legend = mpatches.Patch(color=colors[color_index], label = labels[y_i])
            key_legends.append(legend)
            color_index = (color_index + 1)% len(colors)
        plot.legend(handles=key_legends)
        plot.title('F1 Measure with different thresholds for different algorithms')
        plot.xlabel("Best Threshold Found")
        plot.ylabel("F1 Measure")
        plot.show()





class TesterDataSource:
    def __init__(self):
        db = Globals.database
        try:
            conn = psycopg2.connect(user='root', database=db)
            conn.autocommit = True
        except psycopg2.Error:
            print("Error: cannot connect to event_detection database")
            sys.exit()
        try:
            self.cursor = conn.cursor(cursor_factory=RealDictCursor)
        except psycopg2.Error:
            print("Error: cannot create cursor")
            sys.exit()

        self.query_articles = None
        self.validation_results = None


    #TODO: THIS IS DISGUSTING I HATE JSON UGHH
    def get_algorithms(self):
        """
        gets a list of algorithm ids
        :return: the list
        """

        # self.cursor.execute("SELECT id FROM validation_algorithms")
        # return self.cursor.fetchall()
        algorithm_names = []
        for filename in os.listdir("Validators/"):
            algorithm_file = open("Validators/"+filename)
            algorithm_text = algorithm_file.read()
            algorithm_file.close()
            algorithm_data = json.loads(algorithm_text)
            enabled = algorithm_data["enabled"]
            if enabled:
                algorithm_name = algorithm_data["id"]
                algorithm_names.append(algorithm_name)
            algorithm_file.close()
        algorithm_name_string = "(\'" + "\', \'".join(algorithm_names) + "\')"
        self.cursor.execute("SELECT id, algorithm FROM validation_algorithms WHERE algorithm IN {}".format(algorithm_name_string))
        return self.cursor.fetchall()
        #return[(2, "Swoogle Semantic Analysis"), (4, "TextRank Swoogle Semantic Analysis")]

    def get_queries(self):
        """
        gets a list of query ids
        :return: the list
        """
        self.cursor.execute("SELECT id FROM queries")
        queries = self.cursor.fetchall()
        return [query["id"] for query in queries]

    def get_query_articles(self):
        """
        Gets a list of all query_articles and if the query validates the article
        Caches the list
        :return: the list of query articles
        """
        if self.query_articles is None:
            self.cursor.execute("SELECT query, article, validates FROM query_articles")
            query_articles_list = self.cursor.fetchall()
            self.query_articles = {(qa["query"], qa["article"]) : qa["validates"] for qa in query_articles_list}
        return self.query_articles


    def get_articles(self):
        self.cursor.execute("SELECT id FROM articles")
        articles = self.cursor.fetchall()
        return [article["id"] for article in articles]


    def get_validation_result(self, query_id, article_id, algorithm_id):
        """
        Gets the validation result for a specific query, article and algorithm
        :param query_id: the id of the query
        :param article_id: the id of the article
        :param algorithm_id: the id of the algorithm
        :return: the validation result for the given query id, article id and algorithm id
                 or None if no result found
        """
        # get results from database if we haven't already
        if self.validation_results is None:
            self.get_validation_results()

        # return the validation result if it has been recorded
        if (query_id, article_id, algorithm_id) in self.validation_results:
            return self.validation_results[(query_id, article_id, algorithm_id)]
        else:
            return None

    def get_validation_results(self):
        """
        Creates a dictionary of all validation results and caches it
        :return: the validation results
        """
        if self.validation_results is None:
            self.cursor.execute("SELECT * FROM validation_results")
            results = self.cursor.fetchall()
            # create dictionary with format (query_id, article_id, algorithm_id) -> validates probability
            self.validation_results = {(r["query"], r["article"], r["algorithm"]): r["validates"] for r in results}
        return self.validation_results


def main():
    tester = Tester()
    tester.bootstrap_all()
    tester.test_all()

if __name__ == "__main__":
    main()
