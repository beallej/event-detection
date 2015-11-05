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

          #if cannot find, compare synonym. Stop when found

    


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
    self.title = title
    self.body = body
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
      self.synonyms = self.getSynonyms()
      self.hierarchies = self.getHierarchies()

    def getSynonyms(self):
        return []

    def getHierarchies(self):
        return []

  
class Query:
  
  def __init__(self, id, queryParts, threshold):
    self.threshold = threshold
    self.id = id
    self.subject = QueryElement("subject", queryParts["subject"])
    self.verb = QueryElement("verb", queryParts["verb"])
    self.directObj = QueryElement("directObj", queryParts["directObj"])
    self.indirectObj = QueryElement("indirectObj", queryParts["indirectObj"])
    self.location = QueryElement("location", queryParts["location"])
    self.query = queryParts["query"]
    self.stopList = set(stopwords.words('english'))


    self.queryTagged = self.tagQuery()
    self.synonymsWithTag = {}
    self.getSynonymsWithTag()



  def getId(self):
      return self.id

  def tagQuery(self):
      return pos_tag(word_tokenize(self.query))


  def getSynonymsWithTag(self): #Assume have already
      for taggedWord in self.queryTagged:
        if taggedWord[0].lower() not in self.stopList:
          if taggedWord[1] not in self.synonymsWithTag:
            self.synonymsWithTag[taggedWord[1]] = {}
          self.synonymsWithTag[taggedWord[1]][taggedWord[0]] = []#getoneWordSynonym()


  def getSynonyms(self):
      return self.synonymsWithTag

  def getThreshold(self):
      return self.threshold

  def getElements(self):
      return self.subject, self.verb, self.directObj, self.indirectObj, self.location

class QueryArticleList:

    def __init__(self, query):
        self.query = query
        self.articles = []

    def addArticle(self, article):
        return

    def getNumArticles(self):
        return len(self.articles)
