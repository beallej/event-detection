# This will run on the background all process all newly added query
# Add synonym of new keywords into query_word table
# Potentially, this will clear out query and synonym that are old too
import sys; import os
sys.path.insert(0, os.path.abspath('..'))
sys.path.insert(0, os.path.abspath('.'))

from Utils.DataSource import *
from Keywords_Wordnet.KeywordExtractor import *
from PythonValidators.Validator import Article
import json
import fcntl
import os
import sys
import Globals


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
            if "--no-lock" not in sys.argv:
                path = Globals.articles_path
                fo = open(os.getenv("HOME") + "/.event-detection-active", "wb")
                fd = fo.fileno()
                fcntl.lockf(fd, fcntl.LOCK_EX)
            ds = DataSource()
            unprocessed_articles = ds.get_unprocessed_articles()
            for article in unprocessed_articles:
                try:
                    extractor = KeywordExtractor()
                    article_file = open(os.getcwd()+"/articles/{0}".format(article[2]), "r", encoding="utf8")
                    body = article_file.read()
                    article_file.close()
                    article_with_body = Article(article[1], body, article[3], article[4])
                    keywords = extractor.extract_keywords(article_with_body)
                    keyword_string = json.dumps(keywords)

                    ds.add_keywords_to_article(article[0], keyword_string)
                except (FileNotFoundError, IOError):
                    print("Wrong file or file path")
        finally:
            if "--no-lock" not in sys.argv:
                fcntl.lockf(fd, fcntl.LOCK_UN)
                fo.close()


if __name__ == "__main__":
    ArticleProcessorDaemon().run()
