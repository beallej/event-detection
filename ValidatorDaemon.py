#ValidatorDaemon.py

# Transfering everything in Interface and call validator from here

from Validator import *
import DataSource

ds = DataSource()
unprocessed_pairs = ds.get_unprocessed_query_article_pairs()
for pair in unprocessed_pairs:
	query_id = pair[0]
	article_id = pair[1]
	matching_prob = Validator.validate(query_id, article_id)
	post_validator_update(matching_prob, query_id, article_id)