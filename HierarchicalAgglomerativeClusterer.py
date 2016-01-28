#from matplotlib import pyplot as plt
from scipy.cluster.hierarchy import dendrogram, linkage
from scipy.cluster.hierarchy import fcluster
from Cluster import Cluster
from AbstractClusterer import AbstractClusterer
import numpy as np


class HierarchicalAgglomerativeClusterer(AbstractClusterer):
    """
    Uses hierarchical clustering

    Uses some code from
    https://joernhees.de/blog/2015/08/26/scipy-hierarchical-clustering-and-dendrogram-tutorial/
    """

    def get_cluster_matrix(self, X):
        # generate the linkage matrix
        Z = linkage(X, 'single')
        return Z



    def plot_data(self):
        matrix = self.pre_cluster()
        Z = self.get_cluster_matrix(matrix)
        article_titles = self.matrix_creator.get_article_titles()

        # calculate full dendrogram
        plt.figure(figsize=(10, 10))
        plt.title('Hierarchical Clustering Dendrogram')
        plt.xlabel('distance')
        plt.axhline(y=2.3, c='k')
        dendrogram(
            Z,
            p=5,  # show only the last p merged clusters
            orientation="right",
            labels=article_titles,
            show_leaf_counts=False,  # otherwise numbers in brackets are counts
            #leaf_rotation=90.,  # rotates the x axis labels
            leaf_font_size=9.,  # font size for the x axis labels
        )
        plt.show()

    def cluster(self, k_function = None):
        matrix = self.pre_cluster(k_function)
        Z = self.get_cluster_matrix(matrix)

        clusters = self.get_final_clusters(Z)
        return clusters

    def get_final_clusters(self, Z):
        max_dist = Z[-1][2]
        best_dist = max_dist
        num_clusters = 0
        for i in np.arange(0,max_dist,0.05):
            clusters = self.get_clusters(Z, i)
            if len(clusters) >= num_clusters:
                num_clusters = len(clusters)
                best_dist = i
            else:
                clusters = self.get_clusters(Z, best_dist)
                return clusters
        return []

    def get_final_clusters_max_articles(self, Z):
        max_dist = Z[-1][2]
        best_dist = max_dist
        total_articles_clustered = 0
        for i in np.arange(0,max_dist,0.05):
            clusters = self.get_clusters(Z, i)
            articles_clusters_current = sum(len(cluster.article_titles) for cluster in clusters)
            if articles_clusters_current >= total_articles_clustered:
                total_articles_clustered = articles_clusters_current
                best_dist = i
            elif total_articles_clustered > 0:
                clusters = self.get_clusters(Z, best_dist)
                return clusters
        return []



    def get_clusters(self, Z, distance):
        cluster_matrix = fcluster(Z, distance)
        clusters = {}
        for i in range(len(self.article_ids)):
            cluster_id = cluster_matrix[i]
            clusters.setdefault(cluster_id, Cluster(cluster_id))
            clusters[cluster_id].add_article(self.article_ids[i], self.article_titles[i])
        final_clusters = [v for (k,v) in clusters.items() if v.is_valid_cluster(len(self.article_ids))]
        return final_clusters





def main():
    clusterer = HierarchicalAgglomerativeClusterer()

    clusters = clusterer.cluster()
    for cluster in clusters:
        print(cluster.article_titles)

    #clusterer.plot_data()

if __name__ == '__main__':
    main()
