# Started from "K-means clustering with scipy" from The Glowing Python
# http://glowingpython.blogspot.com/2012/04/k-means-clustering-with-scipy.html

from scipy.cluster.vq import kmeans, vq, whiten
from AbstractClusterer import *
from Cluster import Cluster

class KMeansClusterer(AbstractClusterer):

    def cluster(self, k_function = None):
        matrix = self.pre_cluster(k_function)

        self.whitened_matrix = whiten(matrix)

        # Compute k-means clustering with k clusters
        codebook, distortion1 = kmeans(self.whitened_matrix, self.k)

        # Assign each article title to a cluster
        cluster_ids, distortion2 = vq(self.whitened_matrix, codebook)

        self.print_clusters(cluster_ids)

        clusters = {}
        for i in range(len(self.article_titles)):
            cluster_id = cluster_ids[i]
            clusters.setdefault(cluster_id, Cluster(cluster_id))
            clusters[cluster_id].add_article(self.article_ids[i], self.article_titles[i])
        final_clusters = [v for (k,v) in clusters.items() if v.is_valid_cluster(len(self.article_ids))]
        return final_clusters


    def print_clusters(self, cluster_ids):
        """Prints article titles grouped by cluster"""
        for cluster_id, title in sorted(zip(cluster_ids, self.article_titles)):
            print(cluster_id, title)

def main():
    kmc = KMeansClusterer()
    kmc.cluster()

if __name__ == '__main__':
    main()
