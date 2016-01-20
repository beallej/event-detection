from MatrixCreator import *
from math import sqrt

class AbstractClusterer:
    """
    Abstact Validator: An interface for validators, which validate queries with articles
    """
    def __init__(self):
        """
        obligatory init
        :return: nothing
        """
        pass

    def pre_cluster(self):
        # Cluster by article title words
        self.matrix_creator = MatrixCreator()
        matrix = self.matrix_creator.construct_matrix()
        self.article_titles = self.matrix_creator.get_article_titles()
        return matrix

    def cluster(self):
        assert False

    def get_average_k(self):
        """Computes k with average of rule of thumb k and text databases k.
           Methods developed from:
           https://en.wikipedia.org/wiki/Determining_the_number_of_clusters_in_a_data_set"""
        text_databases_k = self.get_text_databases_k()
        rule_of_thumb_k = self.get_rule_of_thumb_k()
        average_k = round((text_databases_k + rule_of_thumb_k) / 2)
        return average_k

    def get_rule_of_thumb_k(self):
        """Calculates k with square root of (number of documents / 2)"""
        rule_of_thumb_k = round(sqrt(self.matrix_creator.get_num_datapoints() / 2))
        return rule_of_thumb_k

    def get_text_databases_k(self):
        """Calculates k with (m*n)/t, matrix dimensions over number of entries"""
        num_articles = self.matrix_creator.get_num_datapoints()
        num_title_words = self.matrix_creator.get_num_title_words()
        num_entries = self.matrix_creator.get_num_entries()
        text_databases_k = (num_articles * num_title_words) // num_entries
        return text_databases_k