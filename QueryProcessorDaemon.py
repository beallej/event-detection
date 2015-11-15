# This will run on the background all process all newly added query
# Add synonym of new keywords into query_word table
# Potentially, this will clear out query and synonym that are old too

from Validator import Query
from DataSource import *
import fcntl, os

class QueryProcessorDaemon():

	def run(self):
		fd, fo = 0, 0
		try:
			fo = open(os.getenv("HOME") + "/.event-detection-active", "wb")
			fd = fo.fileno()
			fcntl.lockf(fd, fcntl.LOCK_EX)
			ds = DataSource()
			unprocessed_queries = ds.get_unprocessed_queries()
			for query in unprocessed_queries:
				# access into the queries SQL table and find which queries are not process
				THRESHOLD = None
				print(query)
				query_parts = {"query": ' '.join(query[1:6]), "subject": query[1], "verb": query[2],
							  "direct_obj": query[3], "indirect_obj": query[4], "location" : query[5]}
				print(query_parts)
				synonyms = Query(query[0], query_parts, THRESHOLD).get_synonyms()
				print(synonyms)
				# synonyms = {NN: {word1: [list of synonym], word2: [list of synonym],...}, VB..}

				for pos_group in synonyms:
					print(synonyms[pos_group])
					for query_word in synonyms[pos_group]:
						ds.insert_query_word_synonym(query[0], query_word, pos_group, synonyms[pos_group][query_word])

				ds.post_query_processor_update(query[0])
		finally:
			fcntl.lockf(fd, fcntl.LOCK_UN)
			fo.close()

if __name__ == '__main__':
    QueryProcessorDaemon().run()
