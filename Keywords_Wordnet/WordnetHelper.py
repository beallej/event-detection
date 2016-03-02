import sys; import os
sys.path.insert(0, os.path.abspath('..'))
sys.path.insert(0, os.path.abspath('.'))

from nltk.corpus import wordnet
from collections import defaultdict


def get_synonyms(word, pos):
    """
    Gets a set of synonyms of a given word
    :param word: the word
    :param pos: the pos tag
    :return: set of synonyms
    """
    synonyms = set()
    pos = get_pos_tag_for_wordnet(pos)
    # loop over all synsets with correct part-of-speech
    for synset in wordnet.synsets(word, pos):
        for lemma in synset.lemmas():
            synonyms.add(lemma.name())
    return synonyms


def get_hypernyms(word, pos):
    """
    Gets a set of hypernyms of a given word (one level up)
    :param word: the word
    :param pos: the pos tag
    :return: set of hypernyms
    """
    hypernyms = set()
    pos = get_pos_tag_for_wordnet(pos)
    # loop over all synsets with correct part-of-speech
    for synset in wordnet.synsets(word, pos):
        for hypernym in synset.hypernyms():
            for lemma in hypernym.lemmas():
                hypernyms.add(lemma.name())
    return hypernyms


def get_hyponyms(word, pos):
    """
    Gets a set of hyponyms of a given word (one level up)
    :param word: the word
    :param pos: the pos tag
    :return: set of hyponyms
    """
    hyponyms = set()
    pos = get_pos_tag_for_wordnet(pos)
    # loop over all synsets with correct part-of-speech
    for synset in wordnet.synsets(word, pos):
        for hyponym in synset.hyponyms():
            for lemma in hyponym.lemmas():
                hyponyms.add(lemma.name())
    return hyponyms


def get_synonym_list(tagged_sequence):
    """
    Returns a dictionary of sets of synonyms for a tagged sequence of words
    :param tagged_sequence: a sequence of tuples in the form (word, tag)
    :return: a dictionary with synonym sets for each word by POS
    """
    results = defaultdict(dict)
    for word_tag in tagged_sequence:
        word = word_tag[0]
        tag = word_tag[1]
        results[word_tag[1]][word] = get_synonyms(word, tag)
    return results


def get_hypernym_list(tagged_sequence):
    """
    Returns a dictionary of sets of hypernyms for a tagged sequence of words
    :param tagged_sequence: a sequence of tuples in the form (word, tag)
    :return: a dictionary with hypernym sets for each word by POS
    """
    results = defaultdict(dict)
    for word_tag in tagged_sequence:
        word = word_tag[0]
        tag = word_tag[1]
        results[word_tag[1]][word] = get_hypernyms(word, tag)
    return results


def get_hyponym_list(tagged_sequence):
    """
    Returns a dictionary of sets of hyponyms for a tagged sequence of words
    :param tagged_sequence: a sequence of tuples in the form (word, tag)
    :return: a dictionary with hyponym sets for each word by POS
    """
    results = defaultdict(dict)
    for word_tag in tagged_sequence:
        word = word_tag[0]
        tag = word_tag[1]
        results[word_tag[1]][word] = get_hyponyms(word, tag)
    return results


def get_all_related_words(tagged_sequence):
    """
    Returns a dictionary of lists of hypernyms, hyponyms and synonyms
    :param tagged_sequence: a sequence of tuples in the form (word, tag)
    :return: a dictionary with hypernyms, hyponyms and synonyms lists for each word by POS
    """
    synonyms = get_synonym_list(tagged_sequence)
    hypernyms = get_hypernym_list(tagged_sequence)
    hyponyms = get_hyponym_list(tagged_sequence)
    related_words = defaultdict(lambda: defaultdict(set))

    # add all synonyms, hypernyms and hyponyms
    for pos in synonyms:
        for word in synonyms[pos]:
            related_words[pos][word].update(synonyms[pos][word])

    for pos in hypernyms:
        for word in hypernyms[pos]:
            related_words[pos][word].update(hypernyms[pos][word])

    for pos in hyponyms:
        for word in hyponyms[pos]:
            related_words[pos][word].update(hyponyms[pos][word])

    # switch everything from sets to list
    for pos in related_words:
        for word in related_words[pos]:
            related_words[pos][word] = list(related_words[pos][word])

    return related_words


def get_pos_tag_for_wordnet(tag):
    """
    Returns the wordnet version of a part of speech tag if it exists
    wordnet uses a simplified tag system
    :param tag: the NLTK tagger tag
    :return: the wordnet simplified tag
    """
    if tag.startswith("NN"):
        return wordnet.NOUN
    elif tag.startswith("VB"):
        return wordnet.VERB
    elif tag.startswith("JJ"):
        return wordnet.ADJ
    elif tag.startswith("RB"):
        return wordnet.ADV
    else:
        return ""
