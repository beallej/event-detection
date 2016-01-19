
from matplotlib import pyplot as plt
from scipy.cluster.hierarchy import dendrogram, linkage
import numpy as np

from scipy.cluster.hierarchy import fcluster

import numpy
import nltk.corpus

import collections

import DataSource
import json

from collections import Counter

from nltk.stem.snowball import SnowballStemmer
from nltk.stem.wordnet import WordNetLemmatizer

class HACClusterer:
    language = "english"
    def __init__(self):
        self.stopwords = set(nltk.corpus.stopwords.words('english'))
        np.set_printoptions(precision=5, suppress=True)  # suppress scientific float notation
        self.lemmatizer = WordNetLemmatizer()

    def vectorspaced(self, keywords_counter, all_keywords):
        arr = [keywords_counter[word]
                            for word in all_keywords]
        return numpy.array(arr)

    def get_array(self, keywords_counters, all_keywords):
        X = np.array([self.vectorspaced(counter, all_keywords) for counter in keywords_counters])
        return X

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

    def get_clusters(self, Z, cutoff, article_titles):
        #clustered = fcluster(Z, cutoff, criterion='maxclust')
        clustered = fcluster(Z, cutoff)
        clusters = collections.defaultdict(list)
        for i in range(len(article_titles)):
            clusters[clustered[i]].append(article_titles[i])
        final_clusters = []
        for item in clusters:
            if True:
            #if len(article_titles)/4 > len(clusters[item]) > 1:
                print(cutoff, item, clusters[item])
                final_cluster = Cluster(item, clusters[item])
                final_clusters.append(final_cluster)
        return final_clusters

    def extract_keywords_from_blob(self, blob):
        keywords = Counter()
        keywords_pos = json.loads(blob)
        for pos in keywords_pos:
            keyword_list = keywords_pos[pos]
            for keyword in keyword_list:
                stem = self.stemmatize(keyword[0])
                keywords[stem] += float(keyword[1])
        return keywords

    def extract_titles_and_keywords_from_db_result(self, result):
        titles = []
        keywords = []
        for item in result:
            titles.append(item[0])
            keywords.append(self.extract_keywords_from_blob(item[1]))
        self.titles = titles
        self.keywords = keywords
        return titles, keywords

    def stemmatize(self,word):
        """
        stemmatize
        lemmatizes, then stems word
        :param word: word to stem/lemmatize
        :return: stemmed/lemmatized word
        """
        lemma = self.lemmatizer.lemmatize(word)
        stem = (SnowballStemmer(self.language).stem(lemma))
        return stem

    def get_all_keywords(self, keywords):
        all_keywords = set()
        for keyword_list in keywords:
            for keyword in keyword_list:
                all_keywords.add(keyword)
        self.all_keywords = all_keywords
        return all_keywords


class Cluster:
    def __init__(self, id, article_titles):
        self.article_titles = article_titles
        self.id = id

def main():
    ds = DataSource.DataSource()
    clusterer = HACClusterer()
    # titles_and_keywords = ds.get_all_titles_and_keywords()
    # titles, keywords = clusterer.extract_titles_and_keywords_from_db_result(titles_and_keywords)

    # all_keywords = clusterer.get_all_keywords(keywords)
    # X = clusterer.get_array(keywords, all_keywords)
    titles = ds.get_all_ids_and_titles()
    X = clusterer.get_array()
    Z = clusterer.get_cluster_matrix(X)
    # clusters = clusterer.get_clusters(Z, 7, titles)
    # for cluster in clusters:
    #     print(cluster.article_titles)

    #clusterer.plot_data(Z,8 , titles)

if __name__ == '__main__':
    main()



