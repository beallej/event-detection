
import matplotlib.pyplot as plot
import matplotlib.patches as mpatches
from collections import defaultdict
from TestDataSource import *
from AlgorithmTester import *


class Tester:
    def __init__(self):
        self.dataSource = TesterDataSource()
        self.query_articles = self.dataSource.get_query_articles()
        self.results = self.dataSource.get_validation_results()
        self.article_ids = self.dataSource.get_articles()
        self.query_ids = self.dataSource.get_queries()
        self.algorithms = self.dataSource.get_algorithms()
        self.separate_algorithm_data()
        self.alg_testers = []
        for algorithm in self.algorithms:
            self.alg_testers.append(AlgorithmTester(algorithm, self))

    def test_all(self):
        labels = []
        Y_vals = []
        X_vals = []
        for alg_tester in self.alg_testers:
            alg_tester.get_best_threshold_for_algorithm()
            X, Y, f1 = alg_tester.test()
            X_vals.append(X)
            Y_vals.append(Y)
            labels.append(alg_tester.algorithm_name)
        x_label = "Best Threshold Found"
        y_label = "F1 Measure"
        title =  "F1 Measure with different thresholds for different algorithms"
        self.plot_threshold_and_results_multi_algorithm(X_vals, labels, Y_vals, x_label, y_label, title)


    def bootstrap_all(self):
        for alg_tester in self.alg_testers:
            alg_tester.bootstrap()


    def separate_algorithm_data(self):
        algorithm_datasets = defaultdict(dict)
        for algorithm in self.algorithms:
            algorithm_id = algorithm["id"]
            for query_id in self.query_ids:
                for article_id in self.article_ids:
                    algorithm_datasets[algorithm_id][(query_id, article_id)] = self.results[(query_id, article_id, algorithm_id)]
        self.results_by_algorithm = algorithm_datasets

    def plot_threshold_and_results_multi_algorithm(self, x_vals, data_labels, y_vals, x_label, y_label, title):
        colors = ["red", "blue", "yellow", "green", "orange", "purple", "pink"]
        color_index = 0
        key_legends = []
        for y_i, y in enumerate(y_vals):
            plot.scatter(x_vals[y_i], y, color=colors[color_index])

            #If labels for key legend exist
            if data_labels != None:
                legend = mpatches.Patch(color=colors[color_index], label = data_labels[y_i])
                key_legends.append(legend)

            color_index = (color_index + 1)% len(colors)

        #Only display key legend when we want to
        if data_labels != None:
            plot.legend(handles=key_legends)
        plot.title(title)
        plot.xlabel(x_label)
        plot.ylabel(y_label)
        plot.show()



def main():
    tester = Tester()
    # tester.bootstrap_all()
    tester.test_all()

if __name__ == "__main__":
    main()
