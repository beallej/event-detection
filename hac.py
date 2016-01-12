
from matplotlib import pyplot as plt
from scipy.cluster.hierarchy import dendrogram, linkage
import numpy as np

from scipy.cluster.hierarchy import fcluster

import numpy
import nltk.corpus
from nltk.stem.snowball import EnglishStemmer

from random import shuffle
import collections

class HACClusterer:

    def __init__(self):
        self.stemmer_func = EnglishStemmer().stem
        self.stopwords = set(nltk.corpus.stopwords.words('english'))
        np.set_printoptions(precision=5, suppress=True)  # suppress scientific float notation



    def normalize_word(self, word):
        return self.stemmer_func(word.lower())

    def get_words(self, titles):
        words = set()
        for title in titles:
            for word in title.split():
                words.add(self.normalize_word(word))
        return list(words)

    def vectorspaced(self, title, words):
        title_components = [self.normalize_word(word) for word in title.split()]
        return numpy.array([
            word in title_components and not word in self.stopwords
            for word in words], numpy.short)

    def get_article_titles(self, filename):

        with open(filename) as title_file:

            titles = [line.strip() for line in title_file.readlines()]
            shuffle(titles)

            words = self.get_words(titles)

            article_titles = [title for title in titles if title]
            return article_titles

    def get_array_from_titles(self, titles, words):
            X = np.array([self.vectorspaced(title, words) for title in titles if title])
            return X


    def get_cluster_matrix(self, X):
        # generate the linkage matrix
        Z = linkage(X, 'single')
        return Z

    def plot_data(self, Z, cutoff, article_titles):

        # calculate full dendrogram
        plt.figure(figsize=(20, 10))
        #plt.title('Hierarchical Clustering Dendrogram')
        plt.xlabel('distance')
        plt.axhline(y=2.3, c='k')
        dendrogram(
            Z,
            #p=5,  # show only the last p merged clusters
            orientation="bottom",
            labels=article_titles,
            show_leaf_counts=False,  # otherwise numbers in brackets are counts
            leaf_rotation=90.,  # rotates the x axis labels
            leaf_font_size=9.,  # font size for the x axis labels
        )
        plt.show()

    def get_clusters(self, Z, cutoff, article_titles):
        clustered = fcluster(Z, cutoff)
        clusters = collections.defaultdict(list)
        for i in range(len(article_titles)):
            clusters[clustered[i]].append(article_titles[i])
        final_clusters = []
        for item in clusters:
            if len(article_titles)/4 > len(clusters[item]) > 1:
                print(cutoff, item, clusters[item])
                final_cluster = Cluster(item, clusters[item])
                final_clusters.append(final_cluster)
        return final_clusters

class Cluster:
    def __init__(self, id, article_titles):
        self.article_titles = article_titles
        self.id = id

def main():
    clusterer = HACClusterer()
    filename = "article_titles.txt"
    article_titles = clusterer.get_article_titles(filename)
    words = clusterer.get_words(article_titles)
    X = clusterer.get_array_from_titles(article_titles, words)
    Z = clusterer.get_cluster_matrix(X)
    clusters = clusterer.get_clusters(Z, 0.10, article_titles)
    clusterer.plot_data(Z, 0.10, article_titles)

if __name__ == '__main__':
    main()



