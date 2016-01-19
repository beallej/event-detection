from Validator import AbstractValidator, QueryArticleList
import DataSource
from hac import *
import Matrix


class HACValidator(AbstractValidator):
    """
    class HACValidator: Uses keywords from an article to validate a query
    """
    def __init__(self):
        """
        initializes query_article_lists, a list of list of articles defined around a query
        :return: nothing
        """
        self.query_article_lists = []

    def add_query(self, query):
        """
        add_query: adds query to queries to be validated, inits empty query_article_list
        :param query: query to add
        :return: nothing
        """
        query_article_list = QueryArticleList(query)
        self.query_article_lists.append(query_article_list)

    #TODO: WHAT DOES THIS DO
    def add_to_query_article_list(self, article):
        """
        adds to
        :param article: article to add to queries
        :return: list of queries article was added to
        """
        queries_added_to = []
        return queries_added_to

    def get_query_article_lists(self):
        """
        get_query_article_lists
        :return: self.query_article_lists: list of list of articles defined around a query
        """
        return self.query_article_lists

    def validate(self, query_id, article_id):
        """
        validate -- evaluates how much article validates query
        :param query: query to validate
        :param article: article to validate with
        :return: match_percentage (relative measure of how well article validates query)
        """
        max_match_value = 0
        match_value = 0
        # Need to process query and article formats
        ds = DataSource()
        query_synonyms_raw = ds.get_query_synonyms(query_id) # [('and', 'CC', 'Random', []), ('julia', 'NN', 'Random', []), ('phuong', 'JJ', 'Random', []), ('test', 'NN', 'Random', ['trial', 'run', 'mental_test', 'test', 'tryout', 'trial_run', 'exam', 'examination', 'mental_testing', 'psychometric_test']), ('validator', 'NN', 'Random', [])]
		# Convert into {NN: {word1: [list of synonym], word2: [list of synonym],...}, VB..}
        query_synonyms = {}
        for w in query_synonyms_raw:
            if w[1] not in query_synonyms:
                query_synonyms[w[1]] = {}
            query_synonyms[w[1]][w[0]]=w[3]


        clusterer = HACClusterer()
        titles_and_keywords = ds.get_all_titles_and_keywords()
        titles, keywords = clusterer.extract_titles_and_keywords_from_db_result(titles_and_keywords)
        all_keywords = clusterer.get_all_keywords(keywords)
        X = clusterer.get_array(keywords, all_keywords)
        Z = clusterer.get_cluster_matrix(X)
        clusters = clusterer.get_clusters(Z, 0.10, titles)

        for cluster in clusters:




        #print(ds.get_article_keywords(article_id))
        article_keyword = json.loads(ds.get_article_keywords(article_id)[0]) #{NN: [list of keywords], VB:[list of verb keywords]}
        #print(query_synonyms)
        #print(article_keyword)
        for pos in query_synonyms:
            for query_word in query_synonyms[pos]:
                max_match_value += 2
                if pos in article_keyword:
                    article_keyword_with_same_tag = article_keyword[pos][0]
                    #Compare main key. If match, match_value += 2
                    if query_word in article_keyword_with_same_tag:
                        match_value += 2
                    else:
                        for synonym in query_synonyms[pos][query_word]:
                            if synonym in article_keyword_with_same_tag:
                                match_value += 1
                                break
        match_percentage = match_value/max_match_value
        return match_percentage
