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

    MIN_THRESHOLD = 0.1

    def __init__(self, cluster_class, k_function):
        """
        initializes query_article_lists, a list of list of articles defined around a query
        :return: nothing
        """
        self.query_list = []
        clusterer = cluster_class()
        self.clusters = clusterer.cluster()

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

        # Need to process query and article formats
        ds = DataSource()
        query_synonyms_raw = ds.get_query_synonyms(query_id)
        query_synonyms = {}

        for w in query_synonyms_raw:
            query_synonyms[w[0]] = w[3]
        cluster_matches = {}

        for cluster in self.clusters:
            #print(cluster.article_titles)

            max_match_value = 0
            match_value = 0
            cluster_keywords = cluster.get_keywords()
            all_cluster_keywords = set()

            # flatten cluster_keywords
            for pos in cluster_keywords:
                all_cluster_keywords.update(cluster_keywords[pos])

            # find matches
            for query_word in query_synonyms:
                max_match_value += 2
                if query_word in all_cluster_keywords:
                    match_value += 2
                    print(query_word)
                else:
                    synonyms_matched = 0
                    for synonym in query_synonyms[query_word]:
                        if synonym in all_cluster_keywords:
                            match_value += 1
                            #synonyms_matched += 1
                            break
                    #match_value += synonyms_matched/len(query_synonyms[query_word])
            match_percentage = match_value / max_match_value
            cluster_matches[cluster] = match_percentage

        # find the max match_percentage
        max_match_cluster= max(cluster_matches.items(), key=operator.itemgetter(1))[0]
        #print(cluster_matches[max_match_cluster])
        best_value = cluster_matches[max_match_cluster]
        if best_value >= self.MIN_THRESHOLD:
            return max_match_cluster, best_value
        return None, best_value

def main():
    clusterValidator = ClusterValidator(HierarchicalAgglomerativeClusterer, None)
    result, value = clusterValidator.validate(7)
    if result is None:
        print("No clusters found for value " + str(value))
    else:
        print(result.article_titles, value)

if __name__ == "__main__":
    main()
