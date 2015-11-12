# This will run on the background all process all newly added query
# Add synonym of new keywords into query_word table
# Potentially, this will clear out query and synonym that are old too

from Validator import Query
from DataSource import *

class QueryProcessorDaemon():

	def run():
		ds = DataSource()
		unprocessed_queries = ds.get_unprocessed_queries()
		for query in unprocessed_queries:
			# access into the queries SQL table and find which queries are not process
			THRESHOLD = None
			queryParts = {"query": ' '.join(query[1:6]), "subject": query[1], "verb": query[2], 
						  "direct_obj": query[3], "indirect_obj": query[4], "location" : query[5]}
			synonyms = Query(query[0], query_parts, threshold).get_synonyms()
			# synonyms = {NN: {word1: [list of synonym], word2: [list of synonym],...}, VB..}

			for pos_group in synonyms:
				for query_word in pos_group:
					ds.insert_query_word_synonym(query[0], query_word, pos_group, synonyms[pos_group][query_word])
					
			post_query_processor(query[0])

					

