from Validator import QueryArticleList
from DataSource import *
from HierarchicalAgglomerativeClusterer import *
from KMeansClusterer import *
import json
import operator
from AbstractClusterer import *

class ClusterValidator:
    """
    class ClusterValidator: Uses keywords from an article to validate a query
    """

    MIN_THRESHOLD = 0

    def __init__(self, cluster_class, k_function):
        """
        initializes query_article_lists, a list of list of articles defined around a query
        :return: nothing
        """
        self.query_list = []
        clusterer = cluster_class()
        self.clusters = clusterer.cluster(k_function)

    def add_query(self, query):
        """
        add_query: adds query to queries to be validated, inits empty query_article_list
        :param query: query to add
        :return: nothing
        """
        self.query_list.append(query)

    def validate(self, query_id):
        """
        validate -- finds the best cluster to match a query
        :param query: query to get the best cluster for
        :return: a cluster or None (if below a certain threshold)
        """
        max_match_value = 0
        match_value = 0
        # Need to process query and article formats
        ds = DataSource()
        query_synonyms_raw = ds.get_query_synonyms(query_id)
        query_synonyms = {}
        for w in query_synonyms_raw:
            if w[1] not in query_synonyms:
                query_synonyms[w[1]] = {}
            query_synonyms[w[1]][w[0]] = w[3]

        cluster_matches = {}
        for cluster in self.clusters:
            max_match_value = 0
            cluster_keywords = cluster.get_keywords()
            for pos in query_synonyms:
                for query_word in query_synonyms[pos]:
                    max_match_value += 2
                    if pos in cluster_keywords:
                        cluster_keywords_with_same_tag = cluster_keywords[pos][0]
                        #Compare main key. If match, match_value += 2
                        if query_word in cluster_keywords_with_same_tag:
                            match_value += 2
                        else:
                            for synonym in query_synonyms[pos][query_word]:
                                if synonym in cluster_keywords_with_same_tag:
                                    match_value += 1
                                    break
            match_percentage = match_value / max_match_value
            cluster_matches[cluster] = match_percentage

        # find the max match_percentage
        max_match_cluster= max(cluster_matches.items(), key=operator.itemgetter(1))[0]
        print(cluster_matches[max_match_cluster])
        if cluster_matches[max_match_cluster] >= self.MIN_THRESHOLD:
            return max_match_cluster
        return None

def main():
    clusterValidator = ClusterValidator(KMeansClusterer, "every_article_is_cluster_k")
    result = clusterValidator.validate(3)
    if result is None:
        print("No clusters found")
    else:
        print(result.article_titles)

if __name__ == "__main__":
    main()
