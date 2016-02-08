from DataSource import *
import numpy as np
from scipy import mean
import scikits.bootstrap as bootstrap

class Tester:
    def __init__(self):
        self.dataSource = TesterDataSource()

    def test(self):
        self.query_articles = self.dataSource.get_query_articles()
        self.results = self.dataSource.get_validation_results()
        self.article_ids = self.dataSource.get_articles()
        self.query_ids = self.dataSource.get_queries()
        self.algorithm_ids = self.dataSource.get_algorithms()

        self.best_thresholds = {}

        self.bootstrap()
        for algorithm_id in self.article_ids:
            best_threshold = 0
            best_f1 = 0
            for threshold in np.arange(0,1,0.1):
                f1_measures = []
                for article_left_out in self.article_ids:
                    f1_measures.append(self.f1(article_left_out, algorithm_id, threshold))
                avg_f1 = mean(f1_measures)
                if avg_f1 > best_f1:
                    best_f1 = avg_f1
                    best_threshold = threshold
            self.best_thresholds[algorithm_id] = (best_f1, best_threshold)



        #leave one out
        #threshold
        #precision,recall  - bootstrapping and forreal

    #TODO: should we separate out the algorithms???
    def precision_bootstrap(self, dataset):
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

        precision = true_positives/(true_positives + false_positives)
        return precision

        #TODO: should we separate out the algorithms???
    def recall_bootstrap(self, dataset):
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

        recall = true_positives/(true_positives + false_negatives)
        return recall


    def bootstrap(self):

        CIs = bootstrap.ci(data=list(self.results), statfunction=self.precision_bootstrap)
        print("Bootstrapped 95% confidence intervals\nLow:", CIs[0], "\nHigh:", CIs[1])


    #I know this is inneficient we can fix it later
    def f1(self, article_left_out, algorithm_id, threshold):
        true_positives = 0
        false_positives = 0
        false_negatives = 0
        for article_id in self.article_ids:
            if article_id != article_left_out:
                for query_id in self.query_ids:
                    test_value_probability = self.results[(query_id, article_id, algorithm_id)]
                    actual_value = self.query_articles[(query_id, article_id)]
                    test_value = (test_value_probability > threshold)
                    if test_value and actual_value:
                        true_positives += 1
                    elif test_value and not actual_value:
                        false_positives += 1
                    elif not test_value and actual_value:
                        false_negatives += 1

        recall = true_positives/(true_positives + false_negatives)
        precision = true_positives/(true_positives + false_positives)
        f1 = 2 * (precision * recall)/(precision + recall)

        return f1






class TesterDataSource(DataSource):
    def __init__(self):
        super(TesterDataSource, self).__init__()
        self.query_articles = None
        self.validation_results = None

    def get_algorithms(self):
        """
        gets a list of algorithm ids
        :return: the list
        """
        self.cursor.execute("SELECT id FROM validation_algorithms")
        return self.cursor.fetchall()

    def get_queries(self):
        """
        gets a list of query ids
        :return: the list
        """
        self.cursor.execute("SELECT id FROM queries")
        return self.cursor.fetchall()

    def get_query_articles(self):
        """
        Gets a list of all query_articles and if the query validates the article
        Caches the list
        :return: the list of query articles
        """
        if self.query_articles is None:
            self.cursor.execute("SELECT query, article, validates FROM query_articles")
            query_articles_list = self.cursor.fetchall()
            self.query_articles = {(qa[0], qa[1]) : qa[2] for qa in query_articles_list}
        return self.query_articles

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
