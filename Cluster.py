class Cluster:
    def __init__(self, id):
        self.id = id
        self.article_ids = []
        self.article_titles = []

    def add_article(self, article_id, article_title):
        self.article_ids.append(article_id)
        self.article_titles.append(article_title)

    def is_valid_cluster(self, num_articles):
        return num_articles/4 > len(self.article_ids) > 1