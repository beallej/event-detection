# Started from "K-means clustering with scipy" from The Glowing Python
# http://glowingpython.blogspot.com/2012/04/k-means-clustering-with-scipy.html

from scipy.cluster.vq import kmeans, vq, whiten
from AbstractClusterer import *

class KMeansClusterer(AbstractClusterer):



	def cluster(self):

		matrix = self.pre_cluster()

		self.whitened_matrix = whiten(matrix)

		# Compute k
		k = self.get_average_k()

		# Compute k-means clustering with k clusters
		codebook, distortion1 = kmeans(self.whitened_matrix, k)

		# Assign each article title to a cluster
		cluster_ids, distortion2 = vq(self.whitened_matrix, codebook)

		# Print clustering results
		self.print_clusters(cluster_ids)



	def print_clusters(self, cluster_ids):
		"""Prints article titles grouped by cluster"""
		for cluster_id, title in sorted(zip(cluster_ids, self.article_titles)):
			print(cluster_id, title)

def main():
	kmc = KMeansClusterer()
	kmc.cluster()	

if __name__ == '__main__':
	main()


