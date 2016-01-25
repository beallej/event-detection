from numpy import zeros
from DataSource import *
from nltk.stem.snowball import SnowballStemmer
from nltk.stem.wordnet import WordNetLemmatizer
import nltk.corpus
import json
from collections import Counter

# Uncomment following 2 lines to print out full arrays
# import numpy
# numpy.set_printoptions(threshold=numpy.nan)

class MatrixCreator:


    def __init__(self):
        self.ds = DataSource()
        self.ids = []
        self.num_entries = 0
        self.num_datapoints = 0
        self.article_titles = []
        self.article_keywords = []
        self.keywords_by_article = []
        # self.stopwords = set(nltk.corpus.stopwords.words('english'))
        # self.lemmatizer = WordNetLemmatizer()


    def get_num_entries(self):
        '''Gets the number of non-zero entries in the matrix.'''
        return self.num_entries

    def get_num_datapoints(self):
        '''Gets the number of datapoints/rows in the matrix
           (in our case, the number of articles).'''
        return self.num_datapoints

    def get_num_keywords(self):
        '''
        Gets the number of columns in the matrix
        :return:number of columns
        '''
        return self.num_keywords

    def get_article_keywords(self):
        '''Gets ordered list of article titles corresponding to
           article ids in self.ids.'''
        return self.article_keywords

    def get_keywords_by_article(self):
        return self.keywords_by_article

    def get_article_ids(self):
        return self.ids

    def get_article_titles(self):
        return self.article_titles

    # def retrieve_article_ids_and_keywords(self):
    #     '''Gets list of article ids and titles that can be
    #        accessed consistently even if articles are added mid-process.'''
    #     ids = []
    #     titles = []
    #     db_ids_and_titles = self.ds.get_articles()
    #     for item in db_ids_and_titles:
    #         ids.append(item[0])
    #         titles.append(item[1])
    #     self.num_datapoints = len(ids) # Track num datapoints to calculate K
    #     self.ids = ids
    #     self.article_titles = titles
    #     return ids, titles
    #
    # def retrieve_article_ids(self):
    #     '''Gets list of article ids so that keywords and titles can be
    #        accessed consistently even if articles are added mid-process.'''
    #     ids = []
    #     db_ids = self.ds.get_articles()
    #     for id in db_ids:
    #         ids.append(id[0])
    #     self.num_datapoints = len(ids) # Track num datapoints to calculate K
    #     return ids
    # #
    # def retrieve_article_titles(self):
    #     '''Gets ordered list of article titles corresponding to
    #        article ids in self.ids.'''
    #     titles = []
    #     for id in self.ids:
    #         title = self.ds.get_article_title(id)
    #         titles.append(title)
    #     return titles
    #
    # def get_article_keywords(self):
    #     '''Gets dictionary of dictionary of keyword weights
    #        {article_id:{word:weight}}'''
    #     article_keywords = {}
    #     for id in self.ids:
    #         article_keyword_weights = Counter()
    #         db_keywords_str = self.ds.get_article_keywords(id)[0]
    #         db_keywords_dict = json.loads(db_keywords_str)
    #         for pos in db_keywords_dict:
    #             for keyword_with_weight in db_keywords_dict[pos]:
    #                 keyword = keyword_with_weight[0]
    #                 weight = keyword_with_weight[1]
    #                 article_keyword_weights[keyword] = weight
    #         article_keywords[id] = article_keyword_weights
    #     self.article_keywords = article_keywords
    #     return article_keywords
    #
    # def get_keyword_set(self):
    #     '''Gets set of unique keywords across all articles.'''
    #     keyword_set = set()
    #     keywords = self.ds.get_all_article_keywords()
    #     for row in keywords:
    #         article_keyword_dict = json.loads(row[0])
    #         for pos in article_keyword_dict:
    #             pos_keywords = article_keyword_dict[pos]
    #             for keyword_with_weight in pos_keywords:
    #                 keyword = keyword_with_weight[0]
    #                 keyword_set.add(keyword)
    #     return keyword_set

    def retrieve_article_ids_titles_keywords(self):
        articles = self.ds.get_article_ids_titles_keywords()
        self.ids = [article[0] for article in articles]
        self.article_titles = [article[1] for article in articles]
        keywords_by_article = [json.loads(article[2]) for article in articles]
        for id, article_keywords in zip(self.ids, keywords_by_article):
            article_keyword_weights = Counter()
            for pos in article_keywords:
                for keyword_with_weight in article_keywords[pos]:
                    keyword = keyword_with_weight[0]
                    weight = keyword_with_weight[1]
                    article_keyword_weights[keyword] = weight
            self.keywords_by_article.append(article_keyword_weights)

    def retrieve_all_article_keywords(self):
        article_keywords_set = set()
        for article in self.keywords_by_article:
            for keyword in article:
                article_keywords_set.add(keyword)
        self.article_keywords = list(article_keywords_set)

    def construct_matrix(self):
        '''Constructs an articles X title words numpy array and populates it
           with counts in each article.'''

        # Get keywords to construct matrix
        self.retrieve_article_ids_titles_keywords()
        self.retrieve_all_article_keywords()
        self.num_keywords = len(self.article_keywords)
        self.num_datapoints = len(self.ids)
        matrix = zeros((self.num_datapoints, self.num_keywords))
        num_entries = 0
        for keyword_idx, keyword in enumerate(self.article_keywords):
            for article_idx, article_id in enumerate(self.ids):
                if keyword in self.keywords_by_article[article_idx]:
                    matrix[article_idx, keyword_idx] = 1
                    num_entries += 1
        self.num_entries = num_entries # Count num entries to calculate K
        return matrix
