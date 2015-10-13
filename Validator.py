#We use term extraction and clustering methods found in this paper http://nlg18.csie.ntu.edu.tw:8080/lwku/c12.pdf
class Validator :
  def __init__(self, stoplist, multiwordTerms):
    self.stoplist = stoplist
    self.multiwordTerms = multiwordTerms
    self.queries = []

  def addQuery(self, query):
    self.queries.append(query)

  def addToQueries(self, article):
    foundQuery = False
    for query in self.queries:
      if query.matches(article):
        query.addArticle(article)
        foundQuery = True        
        
    #returns true if added to 1 or more queries
    return foundQuery
    

  

class Term: 
  def __init__(self, text, weight):
    self.text = text
    self.weight = weight
    self.substitutes = this.findSubstitutes()
  
  def getSubstitutes(self):
    return self.substitutes
  
  # look up synonyms on wordnet, look up substitutable words in hierarchies (ex. Northfield for Minnesota)
  def findSubstitutes(self):
    # Return a list of substitutes for the term
    substitutes = []
    return substitutes
    
    


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
    
    #N is the number of terms we want to have
    self.terms = self.findTerms(n)
    
  def findTerms(self, n):
    #stem, tag, filter terms
    #assign term weights
    #return n highest-weighted terms
    terms = []
    return terms
  
  def getTerms(self):
    return self.terms

  def isLinkedTo(self, otherArticle):
    #returns true if otherAricle and self are semantically related
    return False

  
  
class Query:
  
  def __init__(self, queryParts, threshold, minArticles):
    self.threshold = threshold
    self.minArticles = minArticles
    self.elements = {}
    
    #Create a dictionary of part-of-speech : Term
    #Example: self.elements["Subject"] = Term("Beyonce", 1)
    #Empty query fields are not part of this new dictionary
    for element in queryParts:
      if self.elements[element] != "":
        self.elements[element] = Term(queryParts[element], 1)
        
    self.articles = []
  
  # Only called if matchesArticle == true
  def addArticle(self, article):
      if articles.length > minArticles:
        return True
  
  def matchesArticle(self, article):
      # if article matches:
        return True
