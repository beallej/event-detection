# This will run on the background all process all newly added query
# Add synonym of new keywords into query_word table
# Potentially, this will clear out query and synonym that are old too

from DataSource import *
import json


class ArticleProcessorDaemon():

	def run(self):
		ds = DataSource()
        extractor = KeywordExtractor()
		unprocessed_articles = ds.get_unprocessed_articles()
		for article in unprocessed_queries:
            keywords = extractor.extract_keywords()
            ds.add_keywords_to_article(article["id"], keyword_string)
QueryProcessorDaemon().run()
