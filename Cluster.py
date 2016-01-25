from DataSource import *
import json
from collections import defaultdict

class Cluster:
    ds = DataSource()

    def __init__(self, id):
        self.id = id
        self.article_ids = []
        self.article_titles = []
        self.keywords = None

    def add_article(self, article_id, article_title):
        self.article_ids.append(article_id)
        self.article_titles.append(article_title)

    def is_valid_cluster(self, num_articles):
        # return num_articles/4 > len(self.article_ids) > 1
        return True
        #return num_articles / 4 > len(self.article_ids)

    def get_keywords(self):
        # don't build keywords dictionary if it has already been built
        final_dict = defaultdict(set)
        if self.keywords is None:
            keyword_dicts = [json.loads(self.ds.get_article_keywords(article)[0])
                            for article in self.article_ids]
            for kw_dict in keyword_dicts:
                for pos in kw_dict:
                    for kw in kw_dict[pos]:
                        final_dict[pos].add(kw[0])
                    #final_dict[pos].update(set(kw_dict[pos]))
        #     # change to lists
            self.keywords = { k : list(v) for k, v in final_dict.items() }
        return self.keywords
