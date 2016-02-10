from DataSource import *
import numpy as np
import scikits.bootstrap as bootstrap
import matplotlib.pyplot as plot
import matplotlib.patches as mpatches
from psycopg2.extras import RealDictCursor
from random import random

class Tester:
    def __init__(self):
        self.dataSource = TesterDataSource()


    #TODO: RIGHT NOW WERE LEAVING ONE ARTICLE OUT BUT WE SHOULD PROBS LEAVE ONE QUERY-ARTICLE PAIR OUT
    def test(self):
        self.query_articles = self.dataSource.get_query_articles()
        self.results = self.dataSource.get_validation_results()
        self.article_ids = self.dataSource.get_articles()
        self.query_ids = self.dataSource.get_queries()
        self.algorithms = self.dataSource.get_algorithms()

        self.best_thresholds = {}

        #self.bootstrap()
        X = np.arange(.1,.4,0.025)
        labels = []
        Y_vals = []
        X_vals = []
        for algorithm in self.algorithms:
            algorithm_id = algorithm[0]
            algorithm_name = algorithm[1]
            labels.append(algorithm_name)
            best_f1_measures = []
            best_thresholds = []
            true_positives = 0
            false_positives = 0
            false_negatives = 0
            for article in self.article_ids:
                for query in self.query_ids:
                    f1_measures = []
                    best_threshold = 0
                    best_f1 = 0
                    for threshold in X:
                        f1_measure = self.f1(article, query, algorithm_id, threshold)
                        f1_measures.append(f1_measure)
                        if f1_measure > best_f1:
                            best_f1 = f1_measure
                            best_threshold = threshold
                    best_f1_measures.append(best_f1)
                    best_thresholds.append(best_threshold)
                    t_p, f_p, f_n = self.validate_query_article_left_out(article, query, algorithm_id, best_threshold)
                    #t_p, f_p, f_n = self.validate_random(article, query)
                    true_positives += t_p
                    false_positives += f_p
                    false_negatives += f_n
            f1 = self.calculate_f1(true_positives, false_positives, false_negatives)
            print(algorithm_name, f1)
        #
        #     X_vals.append(best_thresholds)
        #     Y_vals.append(best_f1_measures)
        # #
        # self.plot_threshold_and_results_multi(X_vals, labels, Y_vals)

    def validate_random(self, article, query):
        actual_value = self.query_articles[(query, article)]
        test_value = random()

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


    #THIS TAKES IN A LIST OF X VALUE LISTS AND A LIST OF Y VALUE LISTS AND A LIST OF KEY LEGENDS
    def plot_threshold_and_results_multi(self, x_vals, labels, y_vals):
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


        #TODO: should we separate out the algorithms???
    def f1_bootstrap(self, dataset):
        true_positives = 0
        false_positives = 0
        false_negatives = 0
        for datum in dataset:
            query_id = datum[0]
            article_id = datum[1]
            algorithm_id = datum[2]
            test_value_probability = self.results[(query_id, article_id, algorithm_id)]
            actual_value = self.query_articles[(query_id, article_id)]

            #TODO: should we use 0 as a threshold here???
            test_value = (test_value_probability > 0.0)
            if test_value and actual_value:
                true_positives += 1
            elif test_value and not actual_value:
                false_positives += 1
            elif not test_value and actual_value:
                false_negatives += 1
        return self.calculate_f1(true_positives, false_positives, false_negatives)


    def bootstrap(self):

        CIs = bootstrap.ci(data=list(self.results), statfunction=self.f1_bootstrap, n_samples=1)
        print("Bootstrapped 95% confidence intervals for recall \nLow:", CIs[0], "\nHigh:", CIs[1])


    def validate_query_article_left_out(self, article_left_out, query_left_out, algorithm_id, threshold):
        test_value_probability = self.results[(query_left_out, article_left_out, algorithm_id)]
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
                if article_id != article_left_out and query_id != query_left_out:
                    test_value_probability = self.results[(query_id, article_id, algorithm_id)]
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


    def get_algorithms(self):
        """
        gets a list of algorithm ids
        :return: the list
        """
        # self.cursor.execute("SELECT id FROM validation_algorithms")
        # return self.cursor.fetchall()

        return[(2, "Swoogle Semantic Analysis"), (4, "TextRank Swoogle Semantic Analysis")]

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
    tester.test()

if __name__ == "__main__":
    main()
