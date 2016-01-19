from numpy import zeros
from DataSource import *
import json
from nltk.stem.snowball import SnowballStemmer
from nltk.stem.wordnet import WordNetLemmatizer
from collections import Counter
import nltk.corpus

# Uncomment following 2 lines to print out full arrays
# import numpy
# numpy.set_printoptions(threshold=numpy.nan)

class Matrix:


    def __init__(self):
        self.ds = DataSource()
        self.ids = []
        self.num_entries = 0
        self.num_datapoints = 0
        self.article_titles = []
        self.title_words_by_article = []
        self.stopwords = set(nltk.corpus.stopwords.words('english'))
        self.lemmatizer = WordNetLemmatizer()


    def get_num_entries(self):
        '''Gets the number of non-zero entries in the matrix.'''
        return self.num_entries

    def get_num_datapoints(self):
        '''Gets the number of datapoints/rows in the matrix
           (in our case, the number of articles).'''
        return self.num_datapoints

    def get_article_titles(self):
        '''Gets ordered list of article titles corresponding to
           article ids in self.ids.'''
        return self.article_titles

    def get_title_words_by_article(self):
        return self.title_words_by_article

    def get_article_ids(self):
        return self.ids

    def retrieve_article_ids_and_titles(self):
        '''Gets list of article ids and titles that can be
           accessed consistently even if articles are added mid-process.'''
        ids = []
        titles = []
        db_ids_and_titles = self.ds.get_articles()
        for item in db_ids_and_titles:
            ids.append(item[0])
            titles.append(item[1])
        self.num_datapoints = len(ids) # Track num datapoints to calculate K
        self.ids = ids
        self.article_titles = titles
        return ids, titles

    def retrieve_article_ids(self):
        '''Gets list of article ids so that keywords and titles can be
           accessed consistently even if articles are added mid-process.'''
        ids = []
        db_ids = self.ds.get_articles()
        for id in db_ids:
            ids.append(id[0])
        self.num_datapoints = len(ids) # Track num datapoints to calculate K
        return ids

    def retrieve_article_titles(self):
        '''Gets ordered list of article titles corresponding to
           article ids in self.ids.'''
        titles = []
        for id in self.ids:
            title = self.ds.get_article_title(id)
            titles.append(title)
        return titles

    def get_title_words_by_article(self):
        '''Gets ordered list [set(stemmed title words)] of article titles
           and set of unique stemmed title words'''
        self.title_words_by_article = []
        self.all_title_words_set = set()
        titles = self.get_article_titles()
        for idx, id in enumerate(self.ids):
            title_words = set()
            title = titles[idx]
            for word in title:
                normalized_word = self.normalize_word(word)
                title_words.add(normalized_word) # For the article
                self.all_title_words_set.add(normalized_word) # Global
            self.title_words_by_article.append(title_words)
        return self.title_words_by_article


    def construct_matrix(self):
        '''Constructs an articles X title words numpy array and populates it
           with counts in each article.'''

        # Initialize article ids and titles
        self.ids = self.retrieve_article_ids()
        self.article_titles = self.retrieve_article_titles()

        # Get keywords to construct matrix
        title_words_by_article = self.get_title_words_by_article()
        all_title_words_list = list(self.all_title_words_set)
        num_title_words = len(all_title_words_list)
        num_articles = len(self.ids)
        matrix = zeros((num_articles, num_title_words))
        num_entries = 0
        for title_word_idx, title_word in enumerate(all_title_words_list):
            for article_idx, article_id in enumerate(self.ids):
                if title_word in title_words_by_article[article_idx]:
                    matrix[article_idx, title_word_idx] = 1
                    num_entries += 1
        self.num_entries = num_entries # Count num entries to calculate K
        return matrix


    def normalize_word(self, word):
        lemma = self.lemmatizer.lemmatize(word.lower())
        stem = (SnowballStemmer("english").stem(lemma))
        return stem

