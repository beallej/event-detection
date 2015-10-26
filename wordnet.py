from nltk.corpus import wordnet

def get_synonyms(word):
	"""Gets a list of synonyms of a given word"""
	synonyms = set()

	# this essentially loops over every meaning of the word
	# we will probably eventually want to figure out which
	# meaning we have in our sentence, and only add those
	# related synonyms
	for synset in wordnet.synsets(word):
		for lemma in synset.lemmas():
			synonyms.add(lemma.name())

	return list(synonyms)

def get_hypernyms(word):
	"""Gets a list of hypernyms of a given word (one level up)"""
	hypernyms = set()

	for synset in wordnet.synsets(word):
		for hypernym in synset.hypernyms():
			for lemma in hypernym.lemmas():
				hypernyms.add(lemma.name())

	return list(hypernyms)
