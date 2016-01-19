
from matplotlib import pyplot as plt
from scipy.cluster.hierarchy import dendrogram, linkage
import numpy as np
from scipy.cluster.hierarchy import fcluster
from Cluster import Cluster


import Matrix
from AbstractClusterer import AbstractClusterer



class HACClusterer(AbstractClusterer):
    """
    Uses hierarchical clustering

    Uses some code from
    https://joernhees.de/blog/2015/08/26/scipy-hierarchical-clustering-and-dendrogram-tutorial/
    """
    def __init__(self):
        super()
        np.set_printoptions(precision=5, suppress=True)  # suppress scientific float notation


    def get_cluster_matrix(self, X):
        # generate the linkage matrix
        Z = linkage(X, 'single')
        return Z


    def plot_data(self, Z, cutoff, article_titles):

        # calculate full dendrogram
        plt.figure(figsize=(10, 10))
        #plt.title('Hierarchical Clustering Dendrogram')
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

    def cluster(self, Z, cutoff, article_titles, article_ids):
        #cluster_matrix = fcluster(Z, cutoff, criterion='maxclust')
        cluster_matrix = fcluster(Z, cutoff)
        clusters = {}
        for i in range(len(article_titles)):
            cluster_id = cluster_matrix[i]
            clusters.setdefault(cluster_id, Cluster(cluster_id))
            clusters[cluster_id].add_article(article_ids[i], article_titles[i])
        final_clusters = [v for (k,v) in clusters.items() if v.is_valid_cluster(len(article_ids))]
        return final_clusters



def main():
    clusterer = HACClusterer()

    m = Matrix.Matrix()
    matrix = m.construct_matrix()
    titles = m.get_article_titles()
    ids = m.get_article_ids()
    Z = clusterer.get_cluster_matrix(matrix)
    clusters = clusterer.cluster(Z, 7, titles, ids)
    for cluster in clusters:
        print(cluster.article_titles)

    #clusterer.plot_data(Z,8 , titles)

if __name__ == '__main__':
    main()



