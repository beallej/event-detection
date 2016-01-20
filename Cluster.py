from DataSource import *
import json
from collections import defaultdict

class Cluster:
    def __init__(self, id):
        self.id = id
        self.article_ids = []
        self.article_titles = []
        self.ds = DataSource()
        self.keywords = None

    def add_article(self, article_id, article_title):
        self.article_ids.append(article_id)
        self.article_titles.append(article_title)

    def is_valid_cluster(self, num_articles):
        return num_articles/4 > len(self.article_ids) > 1

    def get_keywords(self):
        # don't build keywords dictionary if it has already been built
        if self.keywords is None:
            keyword_dicts = [json.loads(self.ds.get_article_keywords(article)[0])
                            for article in self.article_ids]
            merged_dict_sets = defaultdict(set)
            for keyword_dict in keyword_dicts:
                for key, value in keyword_dict.items():
                    # ensure that there are no duplicates
                    merged_dict_sets[key].update(value)
            # change to lists
            self.keywords = { k : list(v) for k, v in merged_dict_sets.items() }
        return self.keywords
