# Started from "K-means clustering with scipy" from The Glowing Python
# http://glowingpython.blogspot.com/2012/04/k-means-clustering-with-scipy.html

from scipy.cluster.vq import kmeans, vq, whiten
from Matrix import *
from math import sqrt

# Cluster by article keywords
m = Matrix()
matrix = m.construct_matrix()
article_titles = m.get_article_titles()
whitened_matrix = whiten(matrix)

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

print("text databases k: ", text_databases_k)
print("rule of thumb k:", rule_of_thumb_k)
k = round((text_databases_k + rule_of_thumb_k) / 2)

# Compute k-means with k clusters
codebook, distortion = kmeans(whitened_matrix, 25)

# Assign each article title to a cluster
cluster_ids, distortion = vq(whitened_matrix, codebook)

# Print article titles grouped by cluster
for cluster_id, title in sorted(zip(cluster_ids, article_titles)):
	print(cluster_id, title)
