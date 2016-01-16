# Started from "K-means clustering with scipy" from The Glowing Python
# http://glowingpython.blogspot.com/2012/04/k-means-clustering-with-scipy.html

from scipy.cluster.vq import kmeans, vq, whiten
from Matrix import *
from math import sqrt

m = Matrix()

article_titles = m.get_article_titles('article_titles.txt')
matrix = m.get_matrix('article_titles.txt')
whitened_matrix = whiten(matrix)

keywords = m.get_keywords()
kwrdmatrix = m.get_keyword_matrix()
whitened_kwrd_matrix = whiten(kwrdmatrix)
whitened_matrix = whitened_kwrd_matrix

# Compute k
# Two methods taken from https://en.wikipedia.org/wiki/Determining_the_number_of_clusters_in_a_data_set


# Method 1: Using (m*n)/t, matrix dimensions over number of entries
shape = whitened_matrix.shape
num_articles = shape[0]
num_keywords = shape[1]
num_entries = m.get_num_entries()
text_databases_k = (num_articles * num_keywords) // num_entries 

# Method 2: Square root of (number of documents / 2)
rule_of_thumb_k = round(sqrt(m.get_num_datapoints()))

# Compute k-means with k clusters
codebook, distortion = kmeans(whitened_matrix, text_databases_k)

# Assign each article title to a cluster
cluster_ids, distortion = vq(whitened_matrix, codebook)

# Print article titles grouped by cluster
for cluster_id, title in sorted(zip(cluster_ids, article_titles)):
	print(cluster_id, title)
