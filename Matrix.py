from collections import Counter
from numpy import zeros

# Uncomment following 2 lines to print out full arrays
# import numpy
# numpy.set_printoptions(threshold=numpy.nan)

class Matrix:

	def __init__(self):
		num_entries = 0

	def get_num_entries(self):
		return self.num_entries

	def get_article_titles(self, filename):
		titles = []
		f = open(filename)
		for line in f.readlines():
			title = line.strip('\n')
			titles.append(title)
		return titles

	def get_vocabulary_set(self, titles):
		vocabulary = set()
		for title in titles:
			for word in title.split():
				vocabulary.add(word.lower())
		return vocabulary

	def get_title_vocabs(self, titles):
		title_vocabs = {} 	
			# title_vocabs is dictionary of dictionary of counters
			# {title:{word:count}}
		for title in titles:
			word_counts = Counter()
			for word in title.split():
				word_counts[word.lower()] += 1
			title_vocabs[title] = word_counts
		return title_vocabs

	def construct_matrix(self, vocabulary_list, titles, title_vocabs):
		num_words = len(vocabulary_list)
		num_titles = len(titles)
		matrix = zeros((num_titles, num_words))
		num_entries_count = 0
		for w_idx, word in enumerate(vocabulary_list):
			for t_idx, title in enumerate(titles):
				if title_vocabs[title][word] > 0:
					matrix[t_idx,w_idx] = title_vocabs[title][word]
					num_entries_count += 1
		self.num_entries = num_entries_count
		return matrix

	def get_matrix(self, filename):
		titles = self.get_article_titles(filename)
		vocabulary_set = self.get_vocabulary_set(titles)
		vocabulary_list = list(vocabulary_set)
		title_vocabs = self.get_title_vocabs(titles)
		matrix = self.construct_matrix(vocabulary_list, titles, title_vocabs)
		return matrix

def main():
	m = Matrix()
	print(m.get_matrix('article_titles.txt'))

if __name__ == '__main__':
	main()