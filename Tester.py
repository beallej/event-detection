from collections import defaultdict
from AlgorithmTester import *
import matplotlib.pyplot as plot
import matplotlib.patches as mpatches
from TestDataSource import *


class Tester:
    def __init__(self):
        """
        Creates a new tester instance and loads necessary data
        :return: None
        """
        self.dataSource = TesterDataSource()
        self.query_articles = self.dataSource.get_query_articles()
        self.results = self.dataSource.get_validation_results()
        self.article_ids = self.dataSource.get_articles()
        self.query_ids = self.dataSource.get_queries()
        self.algorithms = self.dataSource.get_algorithms()
        self.results_by_algorithm = self.dataSource.separate_algorithm_data()
        self.alg_testers = []
        for algorithm in self.algorithms:
            algorithm_id = algorithm["id"]
            algorithm_name = algorithm["name"]
            self.alg_testers.append(AlgorithmTester(algorithm_id, algorithm_name, tester_datasource=self.dataSource))

    def test_all(self):
        """
        Runs test on all algorithms and plots results
        :return: None
        """
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
        title = "F1 Measure with different thresholds for different algorithms"
        self.plot_threshold_and_results_multi_algorithm(X_vals, labels, Y_vals, x_label, y_label, title)

    def bootstrap_all(self):
        """
        Bootstraps data for every algorithm
        :return:
        """
        for alg_tester in self.alg_testers:
            alg_tester.bootstrap()



    @staticmethod
    def plot_threshold_and_results_multi_algorithm(x_vals, data_labels, y_vals, x_label, y_label, title):
        """
        Plots threshold and F1 data for multiple algorithms
        :param x_vals: the X values of the points to plot
        :param data_labels: the labels for the legend
        :param y_vals: the Y values of the points to plot
        :param x_label: the X axis label
        :param y_label: the Y axis label
        :param title: the title for the plot
        :return: None
        """
        colors = ["red", "blue", "yellow", "green", "orange", "purple", "pink"]
        color_index = 0
        key_legends = []
        for y_i, y in enumerate(y_vals):
            plot.scatter(x_vals[y_i], y, color=colors[color_index])

            # If labels for key legend exist
            if data_labels is not None:
                legend = mpatches.Patch(color=colors[color_index], label=data_labels[y_i])
                key_legends.append(legend)

            color_index = (color_index + 1) % len(colors)

        # Only display key legend when we want to
        if data_labels is not None:
            plot.legend(handles=key_legends)
        plot.title(title)
        plot.xlabel(x_label)
        plot.ylabel(y_label)
        plot.show()


def main():
    tester = Tester()
    # bootstrap data
    # tester.bootstrap_all()
    # test all data
    tester.test_all()

if __name__ == "__main__":
    main()
