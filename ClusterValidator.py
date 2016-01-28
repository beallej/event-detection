from HierarchicalAgglomerativeClusterer import *
import operator
from AbstractClusterer import *
from nltk.stem.wordnet import WordNetLemmatizer

class ClusterValidator:
    """
    class ClusterValidator: Uses keywords from an article to validate a query
    """

    MIN_THRESHOLD = 0.1

    def __init__(self):
        """
        initializes query_article_lists, a list of list of articles defined around a query
        :return: nothing
        """
        self.query_list = []
        clusterer = HierarchicalAgglomerativeClusterer()
        self.clusters = clusterer.cluster()
        self.lemmatizer = WordNetLemmatizer()

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
        # if there are no clusters, we can't validate
        if len(self.clusters) == 0:
            return None, 0

        # Need to process query and article formats
        ds = DataSource()
        query_synonyms_raw = ds.get_query_synonyms(query_id)
        query_synonyms = {}

        for w in query_synonyms_raw:
            query_synonyms[self.normalize_keyword(w[0])] = w[3]
        cluster_matches = {}

        for cluster in self.clusters:

            max_match_value = 0
            match_value = 0
            all_cluster_keywords = cluster.get_keywords()
            all_cluster_keywords= set(self.normalize_keyword(kw) for kw in  all_cluster_keywords)

            subkeywords = set()
            for keyword in all_cluster_keywords:
                subkeywords.update(self.normalize_keyword(kw) for kw in keyword.split())

            all_cluster_keywords.update(subkeywords)

            # find matches
            for query_word_raw in query_synonyms:
                query_word = self.normalize_keyword(query_word_raw)
                max_match_value += 2
                if query_word in all_cluster_keywords:
                    match_value += 2
                else:
                    for synonym in query_synonyms[query_word]:
                        if synonym in all_cluster_keywords:
                            match_value += 1
                            break
            match_percentage = match_value / max_match_value
            cluster_matches[cluster] = match_percentage

        # find the max match_percentage
        max_match_cluster = max(cluster_matches.items(), key=operator.itemgetter(1))[0]

        best_value = cluster_matches[max_match_cluster]
        if best_value >= self.MIN_THRESHOLD:
            return max_match_cluster, best_value
        return None, best_value


    def normalize_keyword(self, word):
        lemma = self.lemmatizer.lemmatize(word.lower())
        stem = (SnowballStemmer("english").stem(lemma))
        return stem


def main():
    clusterValidator = ClusterValidator()
    result, value = clusterValidator.validate(7)
    if result is None:
        print("No clusters found for value " + str(value))
    else:
        print(result.article_titles, result.article_ids, value)

if __name__ == "__main__":
    main()
