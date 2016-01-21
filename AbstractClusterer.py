from MatrixCreator import *
from math import sqrt

class AbstractClusterer:
    """
    Abstact Clusterer: An interface for clusters, which cluster articles
    """
    def __init__(self):
        """
        obligatory init
        :return: nothing
        """
        pass

    def pre_cluster(self, k_function = None):
        """
        pre_cluster: creates matrix for clustering, gets article titles and ids
        :return: the matrix for clustering
        """
        # Cluster by article title words
        self.matrix_creator = MatrixCreator()
        matrix = self.matrix_creator.construct_matrix()
        self.article_titles = self.matrix_creator.get_article_titles()
        self.article_ids = self.matrix_creator.get_article_ids()

        if k_function == "get_bigger_k":
            cutoff = self.get_bigger_k()
        elif k_function == "get_rule_of_thumb_k":
            cutoff = self.get_rule_of_thumb_k()
        elif k_function == "get_text_databases_k":
            cutoff = self.get_text_databases_k()
        elif k_function == "every_article_is_cluster_k":
            cutoff = self.every_article_is_cluster_k()
        else:
            cutoff = self.get_average_k()
        self.k = cutoff

        return matrix

    def cluster(self):
        """
        Performs clustering on articles
        :return: a list of clusters
        """
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

    def get_bigger_k(self):
        """Calculates k as 3/4 times the number of articles"""
        num_articles = self.matrix_creator.get_num_datapoints()
        return num_articles * 3 // 4

    def every_article_is_cluster_k(self):
        return self.matrix_creator.get_num_datapoints()