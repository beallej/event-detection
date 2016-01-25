from numpy import zeros
from DataSource import *
from nltk.stem.snowball import SnowballStemmer
from nltk.stem.wordnet import WordNetLemmatizer
import nltk.corpus
from KeywordExtractor import *
import re
import functools

# Uncomment following 2 lines to print out full arrays
# import numpy
# numpy.set_printoptions(threshold=numpy.nan)

class MatrixCreator:


    def __init__(self):
        self.ds = DataSource()
        self.ids = []
        self.num_entries = 0
        self.num_datapoints = 0
        self.num_article_words = 0
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

    def get_num_title_words(self):
        '''
        Gets the number of columns in the matrix
        :return:number of columns
        '''
        return self.num_title_words

    def get_num_article_words(self):
        '''
        Gets the number of unique words across all documents
        :return:number of global words
        '''
        return self.num_article_words

    def get_article_titles(self):
        '''Gets ordered list of article titles corresponding to
           article ids in self.ids.'''
        return self.article_titles

    def get_title_words_by_article(self):
        return self.title_words_by_article

    def get_article_ids(self):
        return self.ids

    def retrieve_article_ids_and_filenames(self):
        '''Gets list of article ids and filenames that can be
           accessed consistently even if articles are added mid-process.'''
        ids = []
        filenames = []
        db_ids_and_filenames = self.ds.get_all_article_ids_and_filenames()
        for item in db_ids_and_filenames:
            ids.append(item[0])
            filenames.append(item[1])
        self.num_datapoints = len(ids) # Track num datapoints to calculate K
        self.ids = ids
        self.filenames = filenames
        return ids, filenames

    def get_article_text_by_article(self):
        '''Gets ordered list [set(stemmed title words)] of article titles
           and set of unique stemmed title words'''
        pattern =  re.compile(r'TITLE:(.*)TEXT:(.*)', re.DOTALL)


        self.article_words_by_article = []
        self.all_article_words_set = set()
        titles = self.get_article_titles()
        for idx, filename in enumerate(self.filenames):
            body = open("articles/" + filename).read()
            article_words = set()
            tagged_items = re.match(pattern, body)
            title_tagged = tagged_items.group(1)
            body_tagged = tagged_items.group(2)

            extractor = KeywordExtractor()
            title_text, _ = extractor.preprocess_keywords(title_tagged)
            body_text, _ = extractor.preprocess_keywords(body_tagged)
            body_text.extend(title_text)

            body_text = [set(sentence.strip().split()) for sentence in body_text]
            body_text_set = functools.reduce(set.union, body_text)

            self.all_article_words_set.update(body_text_set) # Global
            self.article_words_by_article.append(body_text_set)
        self.num_article_words = len(self.all_article_words_set)
        return self.all_article_words_set

    def construct_matrix(self):
        '''Constructs an articles X title words numpy array and populates it
           with counts in each article.'''

        # Initialize article ids and titles
        self.ids, self.filenames = self.retrieve_article_ids_and_filenames()

        # Get keywords to construct matrix
        all_article_words_list = list(self.get_article_text_by_article())
        matrix = zeros((self.num_datapoints, self.num_article_words))
        num_entries = 0
        for article_word_idx, article_word in enumerate(all_article_words_list):
            for article_idx, article_id in enumerate(self.ids):
                if article_word in self.article_words_by_article[article_idx]:
                    matrix[article_idx, article_word_idx] = 1
                    num_entries += 1
        self.num_entries = num_entries # Count num entries to calculate K
        return matrix


    def normalize_word(self, word):
        lemma = self.lemmatizer.lemmatize(word.lower())
        stem = (SnowballStemmer("english").stem(lemma))
        return stem
