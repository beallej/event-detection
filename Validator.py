#We use term extraction and clustering methods found in this paper http://nlg18.csie.ntu.edu.tw:8080/lwku/c12.pdf

from nltk import pos_tag, word_tokenize
from nltk.corpus import stopwords
from KeywordExtractor import *
import re


class AbstractValidator:
  def __init__(self):
      pass

  def validate(self, Query, Article):
      return 0.0


class KeywordValidator(AbstractValidator):
    def __init__(self):
        self.query_article_lists = []

    def add_query(self, query):
        query_article_list = QueryArticleList(query)
        self.query_article_lists.append(query_article_list)

    def add_to_query_article_list(self, article):
        queries_added_to = []
        return queries_added_to

    def get_query_article_lists(self):
        return self.query_article_lists

    def validate(self, Query, Article):
        max_match_value = 0
        match_value = 0
        query_synonyms = Query.get_synonyms() # {NN: {word1: [list of synonym], word2: [list of synonym],...}, VB..}
        article_keyword = Article.get_keyword() #{NN: [list of keywords], VB:[list of verb keywords]}
        for pos in query_synonyms:
            for query_word in query_synonyms[pos]:
                max_match_value += 2
                if pos in article_keyword:
                    article_keyword_with_same_tag = article_keyword[pos]
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


class Source:
    def __init__(self, id, name, reliability):
        self.id = id
        self.name = name
        self.reliability = reliability

    def getID(self):
        # Return source's ID
        return self.id

    def get_name(self):
        # Return source's name
        return self.name

    def get_reliability(self):
        # Return source's reliability
        return self.reliability

    def load_from_SQL(self, id):
        # Return source
        return



class Article:
    def __init__(self, title, body, url, source):
        tagged_items = re.match(r'TITLE:\n(.*)\nTEXT:\n(.*)', body)
        self.title_tagged = tagged_items.group(1)
        self.body_tagged = tagged_items.group(2)
        self.title = title
        self.url = url
        self.source = source
        self.keyword = self.extract_keyword()

    def extract_keyword(self):
        extractor = KeywordExtractor()
        return extractor.extract_keywords(self)

    def get_keyword(self):
        return self.keyword

    def is_linked_to(self, other_article):
        #returns true if otherAricle and self are semantically related
        return False

    def get_title(self):
        return self.title

class QueryElement:
    def __init__(self, role, word):
        self.role = role
        self.word = word
        self.synonyms = self.get_synonyms()
        self.hierarchies = self.get_hierarchies()

    def get_synonyms(self):
        return []

    def get_hierarchies(self):
        return []


class Query:

    def __init__(self, id, query_parts, threshold):
        self.threshold = threshold
        self.id = id
        self.subject = QueryElement("subject", query_parts["subject"])
        self.verb = QueryElement("verb", query_parts["verb"])
        self.direct_obj = QueryElement("direct_obj", query_parts["direct_obj"])
        self.indirect_obj = QueryElement("indirect_obj", query_parts["indirect_obj"])
        self.location = QueryElement("location", query_parts["location"])
        self.query = query_parts["query"]

        stoplist_file = "RAKEtutorialmaster/SmartStoplist.txt"
        self.stop_list = set(open(stoplist_file).readlines())

        self.query_tagged = self.tag_query()
        self.synonyms_with_tag = {}
        self.get_synonyms_with_tag()

    def get_id(self):
        return self.id

    def tag_query(self):
        return pos_tag(word_tokenize(self.query))

    def get_synonyms_with_tag(self): #Assume have already
        for tagged_word in self.query_tagged:
            if tagged_word[0].lower() not in self.stop_list:
                if tagged_word[1] not in self.synonyms_with_tag:
                    self.synonyms_with_tag[tagged_word[1]] = {}
                self.synonyms_with_tag[tagged_word[1]][tagged_word[0]] = []

    def get_synonyms(self):
        return self.synonyms_with_tag

    def get_threshold(self):
        return self.threshold

    def get_elements(self):
        return self.subject, self.verb, self.direct_obj, self.indirect_obj, self.location

class QueryArticleList:
    def __init__(self, query):
        self.query = query
        self.articles = []

    def addArticle(self, article):
        return

    def getNumArticles(self):
        return len(self.articles)
