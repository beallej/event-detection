# This will run on the background all process all newly added query
# Add synonym of new keywords into query_word table
# Potentially, this will clear out query and synonym that are old too

from DataSource import *
from KeywordExtractor import *
import json
from Validator import Article
import pdb

class ArticleProcessorDaemon:
    def run(self):
        ds = DataSource()
        extractor = KeywordExtractor()
        unprocessed_articles = ds.get_unprocessed_articles()
        for article in unprocessed_articles:
            body = open("articles/{0}".format(article[2])).read()
            article_with_body = Article(article[1], body, article[3], article[4])
            keywords = extractor.extract_keywords(article_with_body)
            keyword_string = json.dumps(keywords)
            ds.add_keywords_to_article(article[0], keyword_string)

ArticleProcessorDaemon().run()
