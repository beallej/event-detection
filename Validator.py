#We use term extraction and clustering methods found in this paper http://nlg18.csie.ntu.edu.tw:8080/lwku/c12.pdf

from nltk import pos_tag, word_tokenize
from nltk.corpus import stopwords
from nltk.stem.snowball import SnowballStemmer
import RAKEtutorialmaster.rake as rake
from nltk.stem.wordnet import WordNetLemmatizer
import nltk
import re
from RAKEtutorialmaster.rake  import split_sentences
from data_source import DataSource

ds = DataSource()

class AbstractValidator:
  def __init__(self):
      self.data_source = DataSource()

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
      articleKeyword = Article.getKeywords() #{NN: [list of keywords], VB:[list of verb keywords]}
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

#a word in the text, it's tag, + possible keywords it could be a part of
class KeywordCandidate:
    def __init__(self, word, neighbors, tag):
        self.word = word
        self.contexts = {}
        self.tag = tag
        for neigh in neighbors:
            self.contexts[neigh] = tag

class KeywordExtractor:
    def __init__(self):
        pass
    def extractKeywords(self, article):
        rake_object_title = rake.Rake("RAKEtutorialmaster/SmartStoplist.txt", 4, 3, 1)
        rake_object_body = rake.Rake("RAKEtutorialmaster/SmartStoplist.txt", 4, 3, 3)
        titleKeywords = self.rakeKeywords(article.title, rake_object_title)
        allKeywords = self.rakeKeywords(article.body, rake_object_body)
        for pos in titleKeywords:
            if pos in allKeywords:
                allKeywords[pos].update(titleKeywords[pos])
        for pos in allKeywords:
            allKeywords[pos] = list(allKeywords[pos])
        return allKeywords


    def getNeighbors(self, i, tokens):
         #a keyword is a max of 3 words long -- these are the possible keywords that a stem could belong to
        neighbors = []
        if i - 2 >= 0:
            neighbors.extend([" ".join([tokens[i-2][0].lower(), tokens[i-1][0].lower(), tokens[i][0].lower()]),
                     " ".join([tokens[i-1][0].lower(), tokens[i][0].lower()])])
        elif i - 1 >= 0:
            neighbors.append(" ".join([tokens[i-1][0].lower(), tokens[i][0].lower()]))
        if i + 2 < len(tokens):
            neighbors.extend([" ".join([tokens[i][0].lower(), tokens[i+1][0].lower(), tokens[i+2][0].lower()]),
                              " ".join([tokens[i][0].lower(), tokens[i+1][0].lower()])])
        elif i + 1 < len(tokens):
            neighbors.append(" ".join([tokens[i][0].lower(), tokens[i+1][0].lower()]))
        return neighbors


    def rakeKeywords(self, text, rake_object):
        text = re.sub(r'https?://.+\s', "", text)
        sentence_list_unstemmed = split_sentences(text)
        sentence_list = []
        wnl = WordNetLemmatizer()
        candidate_keywords = {}
        for sentence in sentence_list_unstemmed:
            tokens =  nltk.word_tokenize(sentence)

            #THIS IS WHY I AM SLOW
            tokens_tagged = pos_tag(tokens)
            stemmed = []
            for i in range(len(tokens_tagged)):
                token = tokens_tagged[i]
                word = token[0].lower()
                tag = token[1]
                stem = self.stemmatize(word, wnl)
                stemmed.append(stem)
                neighbors = self.getNeighbors(i, tokens_tagged)
                stem_instance = KeywordCandidate(word, neighbors, tag)

                if stem in candidate_keywords:
                    candidate_keywords[stem].append(stem_instance)
                else:
                    candidate_keywords[stem] = [stem_instance]
            text_stemmed = " ".join(stemmed)
            sentence_list.append(text_stemmed)
        to_run = "! ".join(sentence_list)
        keywords = rake_object.run(to_run)
        return self.tagKeywords(keywords, candidate_keywords)

    def tagKeywords(self, keywords, candidate_keywords):
        #final dictionary
        keywords_tagged = {}

        #(kw, weight)
        for keywordTuple in keywords:
            keyword = keywordTuple[0]

            #i.e. a single word
            if keyword in candidate_keywords:

                #when word showed up
                instances = candidate_keywords[keyword]
                for instance in instances:
                    word = instance.word
                    pos = instance.tag

                    #add instance's pos and actual word
                    if pos not in keywords_tagged:
                        keywords_tagged[pos] = set()
                    keywords_tagged[pos].add(word)
            else:
                #heuristic: if it ends in a noun, likely to be a noun keyword, starts with verb, likely
                #to be verb keyword. If both are true, added to both parts of speech lists

                firstTag, lastTag, unstemmed = self.getMultiwordPos(keyword, candidate_keywords)
                #starts with verb
                if firstTag[0] == 'V':
                    if firstTag not in keywords_tagged:
                        keywords_tagged[firstTag] = set()
                    keywords_tagged[firstTag].add(unstemmed)
                #ends with noun
                if lastTag[0] == 'N':
                    if lastTag not in keywords_tagged:
                        keywords_tagged[lastTag] = set()
                    keywords_tagged[lastTag].add(unstemmed)

        return keywords_tagged

    def getMultiwordPos(self, keyword, candidate_keywords):
        subKeywords = word_tokenize(keyword)
        firstWord = subKeywords[0]
        lastWord = subKeywords[-1]

        firstInstances = candidate_keywords[firstWord]
        lastInstances = candidate_keywords[lastWord]

        firstTag = "XXX"
        lastTag = "XXX"
        unstemmed = "XXX"
        lemmatizer = WordNetLemmatizer()
        for instance in firstInstances:
            for context in instance.contexts:
                context_stemmed = self.stemmatize(context, lemmatizer)
                if keyword == context_stemmed:
                    firstTag = instance.tag
                    unstemmed = context
                    break
        for instance in lastInstances:
            for context in instance.contexts:
                context_stemmed = self.stemmatize(context, lemmatizer)
                if keyword == context_stemmed:
                    lastTag = instance.tag
                    unstemmed = context
                    break
        return firstTag, lastTag, unstemmed

    def stemmatize(self,word, lemmatizer):
        lemma = lemmatizer.lemmatize(word)
        stem = (SnowballStemmer("english").stem(lemma))
        return stem

class Article:
  def __init__(self, title, body, url, source):
    self.title = title
    self.body = body
    self.url = url
    self.source = source
    # self.keyword = self.extractKeywords()
    ds.insert_article_keywords(self.title, self.extractKeywords())

  def extractKeywords(self):
      extractor = KeywordExtractor()
      return extractor.extractKeywords(self)


  def getKeywords(self):
    return ds.get_article_keywords(self.title)
    # return self.keyword

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
  
  def __init__(self, queryParts, threshold):
    self.threshold = threshold
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


  def store(self):
    """Stores the query in the database"""
    test_username = 'Another Person'
    test_phone = '555-222-1234'
    test_email = 'me@test.com'
    user_id = ds.insert_user(test_username, test_phone, test_email)
    ds.insert_query(user_id, self.subject.word, self.verb.word, self.directObj.word, self.indirectObj.word, self.location.word)
    #self.id = ds.get_query_id()


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
