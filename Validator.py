#We use term extraction and clustering methods found in this paper http://nlg18.csie.ntu.edu.tw:8080/lwku/c12.pdf

from nltk import pos_tag, word_tokenize
from nltk.corpus import stopwords
from nltk.stem.snowball import SnowballStemmer
import RAKEtutorialmaster.rake as rake
from nltk.stem.wordnet import WordNetLemmatizer
import nltk
import re
import math
import sys
from RAKEtutorialmaster.rake  import split_sentences_tagged, split_sentences


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

"""
KeywordCandidate: A word in the text, it's tag, + possible keywords it could be a part of
"""
class KeywordCandidate:
    def __init__(self, word, neighbors, tag):
        self.word = word
        self.contexts = {}
        self.tag = tag
        for neigh in neighbors:
            self.contexts[neigh] = tag

"""
KeywordExtractor: Performs keyword extraction on a text
"""
class KeywordExtractor:
    def __init__(self):
        self.lemmatizer = WordNetLemmatizer()

    """
    extract_keywords: main function. performs extraction using RAKE algorithm
    returns {part of speech: [list of keywords with that part of speech]}
    """
    def extract_keywords(self, article):

        #All words in an keyword must contain >= 4 letters
        #All keywords must contain <= 3 words
        #Keywors in title must appear at least once
        #Keywords in body must appear at least c times, where c is depending on the number of characters in the
        #article if the article is short

        count_limit = math.ceil(len(article.body)/2000)
        count_limit = min(count_limit, 3)

        rake_object_title = rake.Rake("RAKEtutorialmaster/SmartStoplist.txt", 4, 3, 1)
        rake_object_body = rake.Rake("RAKEtutorialmaster/SmartStoplist.txt", 4, 3, count_limit)
        all_keywords = self.rake_keywords(article.body, rake_object_body)
        title_keywords = self.rake_keywords(article.title, rake_object_title)

        for pos in title_keywords:
            if pos in all_keywords:
                all_keywords[pos].update(title_keywords[pos])
        for pos in all_keywords:
            all_keywords[pos] = list(all_keywords[pos])
        return all_keywords

    """
    get_neighbors
    returns list of keywords a stem could belong to in this part of the text (a keyword is a max of 3 words long)
    """
    def get_neighbors(self, i, tokens):
         #a keyword is a max of 3 words long -- these are the possible keywords that a stem could belong to
        neighbors = []

        token = tokens[i].split("_")[0]

        def get_neighbors_to_left():
            if i - 1 >= 0:
                token1 = tokens[i-1].split("_")[0]
                token3 = token1 + " " + token
                neighbors.append(token3)
            if i - 2 >= 0:
                token2 = tokens[i-2].split("_")[0]
                token3 = token2 + " " + neighbors[-1]
                neighbors.append(token3)

        def get_neighbors_to_right():
            if i + 1 < len(tokens):
                token1 = tokens[i+1].split("_")[0]
                token3 = token + " " + token1
                neighbors.append(token3)
            if i + 2 < len(tokens):
                token2 = tokens[i+2].split("_")[0]
                token3 = neighbors[-1] + " " + token2
                neighbors.append(token3)

        get_neighbors_to_left()
        get_neighbors_to_right()

        return neighbors

    """
    rake_keywords
    extracts plain text from tagged text.
    stems and lemmatizes text
    performs rake algorithm on that.
    retrieves parts of speech for keywords
    {part of speech: [list of keywords with that part of speech]}
    """
    def rake_keywords(self, text, rake_object):

        #remove url stuff
        text = re.sub(r'https?://.+\s', "", text)

        #split into sentences
        sentence_list_unstemmed = split_sentences_tagged(text)

        sentence_list = []
        candidate_keywords = {}
        for sentence in sentence_list_unstemmed:

            tokens = sentence.split(" ")
            tokens_tagged = []
            for token in tokens:
                if re.search('_', token) != None:
                    tokens_tagged.append(token)

           #preprocess each token
            stemmed = []
            for i in range(len(tokens_tagged)):
                token = tokens_tagged[i]
                word, tag = token.split("_")
                word = word.lower()
                stem = self.stemmatize(word)
                stemmed.append(stem)
                neighbors = self.get_neighbors(i, tokens_tagged)
                stem_instance = KeywordCandidate(word, neighbors, tag)

                if stem in candidate_keywords:
                    candidate_keywords[stem].append(stem_instance)
                else:
                    candidate_keywords[stem] = [stem_instance]
            text_stemmed = " ".join(stemmed)
            sentence_list.append(text_stemmed)

        to_run = "! ".join(sentence_list)

        keywords = rake_object.run(to_run)
        return self.tag_keywords(keywords, candidate_keywords)

    """
    tag_keywords
    retrieves tags and originals from stemmed keywords
    returns {part of speech: [list of keywords with that part of speech]}
    """
    def tag_keywords(self, keywords, candidate_keywords):
        #final dictionary
        keywords_tagged = {}

        #(kw, weight)
        for keyword_tuple in keywords:
            keyword = keyword_tuple[0]

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

                first_tag, last_tag, unstemmed = self.get_multiword_pos(keyword, candidate_keywords)

                #starts with verb
                if first_tag[0] == 'V':
                    if first_tag not in keywords_tagged:
                        keywords_tagged[first_tag] = set()
                    keywords_tagged[first_tag].add(unstemmed)

                #ends with noun
                if last_tag[0] == 'N':
                    if last_tag not in keywords_tagged:
                        keywords_tagged[last_tag] = set()
                    keywords_tagged[last_tag].add(unstemmed)

        return keywords_tagged

    """
    get_tag_and_original_from_keyword
    gets tag of a specific word within keyword and original keyword from stemmed multi-word keyword
    returns tag, original keyword
    """
    def get_tag_and_original_from_keyword(self, keyword, keyword_instances):
        for instance in keyword_instances:
            for context in instance.contexts:

                #stem, lemmatize, lower every word in context
                context_stemmed = " ".join(map(self.stemmatize, context.lower().split(" ")))
                if keyword == context_stemmed:
                    return instance.tag, context
        return "XXX", None


    """
    get_multiword_pos
    for a given multi-word keyword, gets original unstemmed keyword, and pos of first and last word
    XXX for undeterminable POS
    returns first tag, last tag, unstemmed keyword
    """
    def get_multiword_pos(self, keyword, candidate_keywords):
        sub_keywords = word_tokenize(keyword)
        first_word = sub_keywords[0]
        last_word = sub_keywords[-1]

        #instances when first word showed up
        first_instances = candidate_keywords[first_word]

        #instances when last word showed up
        last_instances = candidate_keywords[last_word]

        first_tag, first_unstemmed = self.get_tag_and_original_from_keyword(keyword, first_instances)
        last_tag, last_unstemmed = self.get_tag_and_original_from_keyword(keyword, last_instances)
        unstemmed = first_unstemmed if first_unstemmed != None else last_unstemmed if last_unstemmed != None else "XXX"

        return first_tag, last_tag, unstemmed

    """
    stemmatize
    lemmatizes, then stems word
    """
    def stemmatize(self,word):
        lemma = self.lemmatizer.lemmatize(word)
        stem = (SnowballStemmer("english").stem(lemma))
        return stem

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
