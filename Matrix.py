from collections import Counter
from numpy import zeros
from DataSource import *
import json

# Uncomment following 2 lines to print out full arrays
# import numpy
# numpy.set_printoptions(threshold=numpy.nan)

class Matrix:

	def __init__(self):
		self.ds = DataSource()
		self.ids = []
		self.num_entries = 0
		self.num_datapoints = 0

	def get_num_entries(self):
		'''Gets the number of non-zero entries in the matrix.'''
		return self.num_entries

	def get_num_datapoints(self):
		'''Gets the number of datapoints/rows in the matrix 
		   (in our case, the number of articles).'''
		return self.num_datapoints

	def retrieve_article_ids(self):
		'''Gets list of article ids so that keywords and titles can be 
		   accessed consistently even if articles are added mid-process.'''
		ids = []
		db_ids = self.ds.get_articles()
		for id in db_ids:
			ids.append(id[0])
		self.num_datapoints = len(ids) # Track num datapoints to calculate K
		return ids

	def get_article_titles(self):
		'''Gets ordered list of article titles corresponding to 
		   article ids in self.ids.'''
		titles = []
		for id in self.ids:
			title = self.ds.get_article_title(id)
			titles.append(title)
		return titles

	def get_article_keywords(self):
		'''Gets dictionary of dictionary of keyword weights
		   {article_id:{word:weight}}'''
		article_keywords = {}
		for id in self.ids:
			article_keyword_weights = {}
			db_keywords_str = self.ds.get_article_keywords(id)[0]
			db_keywords_dict = json.loads(db_keywords_str)
			for pos in db_keywords_dict:
				for keyword_with_weight in pos_keywords:
					keyword = keyword_with_weight[0]
					weight = keyword_with_weight[1]
					article_keyword_weights[keyword] = weight
			article_keywords[id] = article_keyword_weights
		return article_keywords

	def get_keyword_set(self):
		'''Gets set of unique keywords across all articles.'''
		keyword_set = set()
		keywords = self.ds.get_all_article_keywords()
		for row in keywords:
			article_keyword_dict = json.loads(row)
			for pos in article_keyword_dict:
				pos_keywords = article_keyword_dict[pos]
				for keyword_with_weight in pos_keywords:
					keyword = keyword_with_weight[0]
					keyword_set.add(keyword)
		return keyword_set

	def construct_matrix(self, keyword_list, article_ids, article_keywords):
		'''Constructs an articles X keywords numpy array and populates it 
		   with keyword weights in each article.'''
		num_keywords = len(keyword_list)
		num_articles = len(article_ids)
		matrix = zeros((num_articles, num_keywords))
		num_entries = 0
		for kword_idx, keyword in enumerate(keyword_list):
			for article_idx, article_id in enumerate(article_ids):
				if article_keywords[article_id][keyword] > 0:
					matrix[article_idx, kword_idx] = article_keywords[article_id][keyword]
					num_entries += 1
		self.num_entries = num_entries # Count num entries to calculate K
		return matrix

	def get_keyword_matrix(self):
		self.ids = self.retrieve_article_ids()
		article_titles = self.get_article_titles()
		article_keywords = self.get_article_keywords()
		keyword_set = self.get_keyword_set()
		keyword_list = list(keyword_set)
		matrix = self.construct_matrix(keyword_list, article_ids, article_keywords)

if __name__ == '__main__':
	main()