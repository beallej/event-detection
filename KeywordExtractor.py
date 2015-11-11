from nltk.stem.snowball import SnowballStemmer
import RAKEtutorialmaster.rake as rake
from nltk.stem.wordnet import WordNetLemmatizer
import re
import math
from RAKEtutorialmaster.rake  import split_sentences_tagged



class KeywordCandidate:
    """
    KeywordCandidate: A word in the text, it's tag, + possible keywords it could be a part of
    """
    def __init__(self, word, neighbors, tag):
        self.word = word
        self.contexts = {}
        self.tag = tag
        for neigh in neighbors:
            self.contexts[neigh] = tag




class KeywordExtractor:
    """
    KeywordExtractor: Performs keyword extraction on a text
    """
    stoplist_file = "RAKEtutorialmaster/SmartStoplist.txt"
    language = "english"
    max_words_in_keyword = 3
    min_letters_in_word_in_keyword = 4
    min_occurrences_body = 3
    min_occurrences_title = 1

    def __init__(self):
        """
        Inits lemmatizer for Keyword extraction
        :return: None
        """
        self.lemmatizer = WordNetLemmatizer()

    def extract_keywords(self, article):
        """
        extract_keywords: main function. performs extraction using RAKE algorithm
        :param article: article object to get keywords from
        :return:  {part of speech: [list of keywords with that part of speech]}
        """

        #Keywords in body must appear at least a certain number of  times, depending on the number of
        #characters in the article if the article is short

        self.min_occurrences_body = min(self.min_occurrences_body, math.ceil(len(article.body_tagged)/2000))

        rake_object_title = rake.Rake(self.stoplist_file, self.min_letters_in_word_in_keyword, self.max_words_in_keyword, self.min_occurrences_title)
        rake_object_body = rake.Rake(self.stoplist_file, self.min_letters_in_word_in_keyword, self.max_words_in_keyword, self.min_occurrences_body)

        preprossed_title, title_keyword_candidates = self.preprocess_keywords(article.title_tagged)
        preprossed_body, body_keyword_candidates = self.preprocess_keywords(article.body_tagged)

        title_keywords = rake_object_title.run(preprossed_title)
        body_keywords = rake_object_body.run(preprossed_body)

        title_keywords_with_tags = self.get_tags_for_keywords(title_keywords, title_keyword_candidates)
        all_keywords_with_tags = self.get_tags_for_keywords(body_keywords, body_keyword_candidates)

        for pos in title_keywords_with_tags:
            if pos in all_keywords_with_tags:
                all_keywords_with_tags[pos] = all_keywords_with_tags[pos].union(title_keywords_with_tags[pos])
        for pos in all_keywords_with_tags:
            all_keywords_with_tags[pos] = list(all_keywords_with_tags[pos])
        return all_keywords_with_tags



    def get_neighbors(self, i, tokens):
        """
        get_neighbors
        returns list of keywords a stem could belong to in this part of the text, for example:
        For a stem "cat" in "Fluffy cats like to play with string", the neighbors of "cat' would be:
        ["Fluffy cats like", "fluffy cats", "cats like", "cats like to"]
        :param i: index of stem within list of tokens
        :param tokens: tokens of a sentence
        :return: list of keywords the stem could belong to
        """

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

    def preprocess_keywords(self, text):
        """
        preprocess_keywords
        extracts plain text from tagged text, saving tags for possible later lookup
        stems and lemmatizes text
        :param text: raw tagged article text
        :return: text to perform keyword extraction on, dictionary of possible keywords for a stem {stem : [possible keywords]}
        """

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
        return to_run, candidate_keywords


    def get_tags_for_keywords(self, keywords, candidate_keywords):
        """
        get_tags_for_keywords
        retrieves tags and originals from stemmed keywords
        :param keywords: keywords extracted from rake algorithm-- a list of tuples
        of the form (keyword, weight)
        :param candidate_keywords: dictionary of possible keywords for a stem {stem : [possible keywords]}
        :return: dictionary of tag to keywords with that tag, ex {NN: [cat, dog]}
        """

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

                #starts with verb (non gerundal), probably a verb --> ex. "take shelter", NOT "taking a bath"
                if first_tag[0] == 'V' and first_tag != "VBG":
                    if first_tag not in keywords_tagged:
                        keywords_tagged[first_tag] = set()
                    keywords_tagged[first_tag].add(unstemmed)

                #ends with noun or gerund, probably a noun --> ex. "long-distance running", "cat's pajamas"
                if last_tag[0] == 'N' or last_tag == "VBG":
                    if last_tag not in keywords_tagged:
                        keywords_tagged[last_tag] = set()
                    keywords_tagged[last_tag].add(unstemmed)

        return keywords_tagged


    def get_tag_and_original_from_keyword(self, keyword, keyword_instances):
        """
        get_tag_and_original_from_keyword
        gets tag of a specific word within keyword and original keyword from stemmed multi-word keyword
        :param keyword: stemmed keyword
        :param keyword_instances: instances in which that stem appears in original text
        :return: tag, original keyword. If not found, returns "XXX", None
        """
        for instance in keyword_instances:
            for context in instance.contexts:

                #stem, lemmatize, lower every word in context
                context_stemmed = " ".join(map(self.stemmatize, context.lower().split(" ")))
                if keyword == context_stemmed:
                    return instance.tag, context
        return "XXX", None



    def get_multiword_pos(self, keyword, candidate_keywords):
        """
        get_multiword_pos
        for a given multi-word keyword, gets original unstemmed keyword, and pos of first and last word
        XXX for undeterminable POS
        :param keyword: stemmed keyword (with more than one word in it, (ex. the cat pajama for the cat's pajamas)
        :param candidate_keywords: dictionary of possible keywords for a stem {stem : [possible keywords]}
        :return: tag of first word (or "XXX" for unknown), tag of last word(or "XXX" for unknown), unstemmed version of keyword

        """
        sub_keywords = keyword.split(" ")
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


    def stemmatize(self,word):
        """
        stemmatize
        lemmatizes, then stems word
        :param word: word to stem/lemmatize
        :return: stemmed/lemmatized word
        """
        lemma = self.lemmatizer.lemmatize(word)
        stem = (SnowballStemmer(self.language).stem(lemma))
        return stem