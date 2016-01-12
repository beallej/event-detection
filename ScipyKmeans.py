# Started from "K-means clustering with scipy" from The Glowing Python
# http://glowingpython.blogspot.com/2012/04/k-means-clustering-with-scipy.html

from scipy.cluster.vq import kmeans, vq, whiten
from Matrix import get_matrix, get_article_titles

article_titles = get_article_titles('article_titles.txt')
matrix = get_matrix('article_titles.txt')
whitened_matrix = whiten(matrix)

# Compute k-means with k clusters
k = 10
codebook, distortion = kmeans(whitened_matrix, k)

# Assign each article title to a cluster
cluster_ids, distortion = vq(whitened_matrix, codebook)

# Print article titles grouped by cluster
for cluster_id, title in sorted(zip(cluster_ids, article_titles)):
	print(cluster_id, title)