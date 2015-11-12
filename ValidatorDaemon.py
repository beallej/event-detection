#ValidatorDaemon.py

# Transfering everything in Interface and call validator from here

from Validator import *
from DataSource import *

class ValidatorDaemon():

	def run(self):
		ds = DataSource()
		unprocessed_pairs = ds.get_unprocessed_query_article_pairs()
		for pair in unprocessed_pairs:
			query_id = pair[0]
			article_id = pair[1]
			matching_prob = KeywordValidator().validate(query_id, article_id)
			ds.post_validator_update(matching_prob, query_id, article_id)

ValidatorDaemon().run()