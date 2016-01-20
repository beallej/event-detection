
from matplotlib import pyplot as plt
from scipy.cluster.hierarchy import dendrogram, linkage
from scipy.cluster.hierarchy import fcluster
from Cluster import Cluster
from AbstractClusterer import AbstractClusterer



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

    def cluster(self):
        matrix = self.pre_cluster()
        Z = self.get_cluster_matrix(matrix)
        article_ids = self.matrix_creator.get_article_ids()
        article_titles = self.matrix_creator.get_article_titles()
        cutoff = self.get_average_k()
        cluster_matrix = fcluster(Z, cutoff, criterion='maxclust')
        clusters = {}
        for i in range(len(article_titles)):
            cluster_id = cluster_matrix[i]
            clusters.setdefault(cluster_id, Cluster(cluster_id))
            clusters[cluster_id].add_article(article_ids[i], article_titles[i])
        final_clusters = [v for (k,v) in clusters.items() if v.is_valid_cluster(len(article_ids))]
        return final_clusters



def main():
    clusterer = HierarchicalAgglomerativeClusterer()

    clusters = clusterer.cluster()
    for cluster in clusters:
        print(cluster.article_titles)

    clusterer.plot_data()

if __name__ == '__main__':
    main()



