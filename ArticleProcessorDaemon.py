# This will run on the background all process all newly added query
# Add synonym of new keywords into query_word table
# Potentially, this will clear out query and synonym that are old too

from DataSource import *
from KeywordExtractor import *
from Validator import Article
import json
import fcntl
import os


class ArticleProcessorDaemon:
    """
    class QueryArticleList -- processes articles to add keywords
    """
    def run(self):
        """
        adds keywords as a JSON string to articles in database
        :return: Nothing
        """
        fd, fo = 0, 0
        try:
            fo = open(os.getenv("HOME") + "/.event-detection-active", "wb")
            fd = fo.fileno()
            fcntl.lockf(fd, fcntl.LOCK_EX)
            ds = DataSource()
            extractor = KeywordExtractor()
            unprocessed_articles = ds.get_unprocessed_articles()
            for article in unprocessed_articles:
                body = open("articles/{0}".format(article[2])).read()
                article_with_body = Article(article[1], body, article[3], article[4])
                keywords = extractor.extract_keywords(article_with_body)

                keyword_string = json.dumps(keywords)
                print(keyword_string)
                ds.add_keywords_to_article(article[0], keyword_string)
        finally:
            fcntl.lockf(fd, fcntl.LOCK_UN)
            fo.close()

if __name__ == "__main__":
    ArticleProcessorDaemon().run()
