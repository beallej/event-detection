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
    #returns true if otherAricle and self are semantically related
    return False
  def getTitle(self):
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
