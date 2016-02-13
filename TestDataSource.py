import psycopg2
import sys
from psycopg2.extras import RealDictCursor
import Globals
from Collections import defaultdict

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
        self.validation_ratio = None

    def get_validation_ratio(self):
        if self.validation_ratio is None:
            self.cursor.execute("SELECT count(*) FROM query_articles where validates = true")
            validated = self.cursor.fetchone()
            self.cursor.execute("SELECT count(*) FROM query_articles")
            total = self.cursor.fetchone()
            self.validation_ratio = validated["count"]/total["count"]
        return self.validation_ratio

    def get_algorithms(self):
        """
        gets a list of algorithm ids
        :return: the list
        """
        if self.algorithms is None:
            self.cursor.execute("SELECT id, algorithm FROM validation_algorithms WHERE enabled = true")
            self.algorithms = self.cursor.fetchall()
        return self.algorithms

    def get_queries(self):
        """
        gets a list of query ids
        :return: the list
        """
        if self.queries is None:
            self.cursor.execute("SELECT id FROM queries")
            queries = self.cursor.fetchall()
            self.queries = [query["id"] for query in queries]
        return self.queries

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
        if self.articles is None:
            self.cursor.execute("SELECT id FROM articles")
            articles = self.cursor.fetchall()
            self.articles = [article["id"] for article in articles]
        return self.articles


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

    def separate_algorithm_data(self):
        """
        separates out validation data by the algorithm used to validate
        :return: dictionary: {algorithm id: {(query id, article id) : validation value)}}
        """
        algorithm_datasets = defaultdict(dict)
        results = self.get_validation_results()
        for algorithm in self.get_algorithms():
            algorithm_id = algorithm["id"]
            for query_id in self.get_queries():
                for article_id in self.get_articles():
                    algorithm_datasets[algorithm_id][(query_id, article_id)] = results[(query_id, article_id, algorithm_id)]
        self.results_by_algorithm = algorithm_datasets

    def get_results_by_algorithms(self, algorithm_id):
        """
        Gets return validation data for a given algorithms
        :param algorithm_id: id of algorithm of interest
        :return: dictionary: {(query id, article id) : validation value)}
        """
        if self.results_by_algorithm is None:
            self.separate_algorithm_data()
        return self.results_by_algorithm[algorithm_id]
