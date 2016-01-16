from Validator import *
from DataSource import *
from Notifier import *


class ValidatorDaemon:
    """
    class QueryArticleList -- attempts to match queries to articles using
    keyword validator
    """
    def run(self):
        """
        attempts to validate all pairs of queries and articles that have not yet
        been run through the validator
        :return: Nothing
        """
        notifier = Notifier()
        ds = DataSource()
        unprocessed_pairs = ds.get_unprocessed_query_article_pairs()
        for pair in unprocessed_pairs:
            query_id = pair[0]
            article_id = pair[1]
            if ds.article_processed(article_id):
                matching_prob = KeywordValidator().validate(query_id, article_id)
                ds.post_validator_update(matching_prob, query_id, article_id)
                if matching_prob > 0.2:
                    notifier.on_validation(query_id, article_id)

if __name__ == "__main__":
    ValidatorDaemon().run()
