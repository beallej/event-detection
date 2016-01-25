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
