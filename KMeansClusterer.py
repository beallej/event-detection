# Started from "K-means clustering with scipy" from The Glowing Python
# http://glowingpython.blogspot.com/2012/04/k-means-clustering-with-scipy.html

from scipy.cluster.vq import kmeans, vq, whiten
from Matrix import *
from math import sqrt

class KMeansClusterer():

	def cluster(self): # TODO add taking in parameter for type of k calculation

		# Cluster by article title words
		m = Matrix()
		matrix = m.construct_matrix()
		article_titles = m.get_article_titles()
		whitened_matrix = whiten(matrix)

		# Compute k
		k = self.get_average_k(m, whitened_matrix)

		# Compute k-means clustering with k clusters
		codebook, distortion = kmeans(whitened_matrix, k)

		# Assign each article title to a cluster
		cluster_ids, distortion = vq(whitened_matrix, codebook)

		# Print clustering results
		self.print_clusters(cluster_ids, article_titles)

	def get_average_k(self, matrix, whitened_matrix):
		"""Computes k with average of rule of thumb k and text databases k.
		   Methods developed from:
		   https://en.wikipedia.org/wiki/Determining_the_number_of_clusters_in_a_data_set"""
		text_databases_k = self.get_text_databases_k(matrix, whitened_matrix.shape)
		rule_of_thumb_k = self.get_rule_of_thumb_k(matrix)
		average_k = round((text_databases_k + rule_of_thumb_k) / 2)
		return average_k

	def get_rule_of_thumb_k(self, matrix):
		"""Calculates k with square root of (number of documents / 2)"""
		rule_of_thumb_k = round(sqrt(matrix.get_num_datapoints()))
		return rule_of_thumb_k

	def get_text_databases_k(self, matrix, shape):
		"""Calculates k with (m*n)/t, matrix dimensions over number of entries"""
		num_articles = shape[0]
		num_keywords = shape[1]
		num_entries = matrix.get_num_entries()
		text_databases_k = (num_articles * num_keywords) // num_entries 
		return text_databases_k

	def print_clusters(self, cluster_ids, article_titles):
		"""Prints article titles grouped by cluster"""
		for cluster_id, title in sorted(zip(cluster_ids, article_titles)):
			print(cluster_id, title)

def main():
	kmc = KMeansClusterer()
	kmc.cluster()	

if __name__ == '__main__':
	main()


