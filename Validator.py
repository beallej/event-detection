#We use term extraction and clustering methods found in this paper http://nlg18.csie.ntu.edu.tw:8080/lwku/c12.pdf

from nltk import pos_tag, word_tokenize
from nltk.corpus import stopwords
from KeywordExtractor import *


class AbstractValidator:
  def __init__(self):
      pass
  def validate(self, Query, Article):
      return 0.0


class KeywordValidator(AbstractValidator):
  def __init__(self):
    self.queryArticleLists = []

  def addQuery(self, query):
    queryArticleList = QueryArticleList(query)
    self.queryArticleLists.append(queryArticleList)

  def addToQueryArticleList(self, article):
      queriesAddedTo = []
      return queriesAddedTo

  def getQueryArticleLists(self):
      return self.queryArticleLists

  def validate(self, Query, Article):
      maxMatchValue = 0
      matchValue = 0
      querySynonyms = Query.getSynonyms() # {NN: {word1: [list of synonym], word2: [list of synonym],...}, VB..}
      articleKeyword = Article.getKeyword() #{NN: [list of keywords], VB:[list of verb keywords]}
      for pos in querySynonyms:

        for queryWord in querySynonyms[pos]:
          maxMatchValue += 2
          if pos in articleKeyword:
            articleKeywordWithSameTag = articleKeyword[pos]
            #Compare main key. If match, matchValue += 2
            if queryWord in articleKeywordWithSameTag:
              matchValue += 2
            else:
              for synonym in querySynonyms[pos][queryWord]:
                if synonym in articleKeywordWithSameTag:
                  matchValue += 1
                  break
      matchPercentage = matchValue/maxMatchValue
      return matchPercentage

          #if cannot find, compare synonym. Stop when found




class Source:
  def __init__(self, id, name, reliability):
    self.id = id
    self.name = name
    self.reliability = reliability

  def getID(self):
    # Return source's ID
    return self.id

  def getName(self):
    # Return source's name
    return self.name

  def getReliability(self):
    # Return source's reliability
    return self.reliability

  def loadFromSQL(self, id):
    # Return source
    return



class Article:
  def __init__(self, title, body, url, source):
    self.title = title
    self.body = body
    self.url = url
    self.source = source
    self.keyword = self.extractKeyword()

  def extractKeyword(self):
      extractor = KeywordExtractor()
      return extractor.extract_keywords(self)


  def getKeyword(self):
    return self.keyword

  def isLinkedTo(self, otherArticle):
    """returns true if otherAricle and self are semantically related"""
    return False
  def getTitle(self):
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
    self.subject = QueryElement("subject", queryParts["subject"])
    self.verb = QueryElement("verb", queryParts["verb"])
    self.direct_obj = QueryElement("direct_obj", queryParts["direct_obj"])
    self.indirect_obj = QueryElement("indirect_obj", queryParts["indirect_obj"])
    self.location = QueryElement("location", queryParts["location"])
    self.query = query_parts["query"]
    self.stop_list = set(stopwords.words("english"))

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
          if taggedWord[1] not in self.synonyms_with_tag:
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
