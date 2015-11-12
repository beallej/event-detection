#!/usr/bin/python

# import cgitb
# cgitb.enable()

import psycopg2
import sys
import re

class DataSource:
    
    def __init__(self):
        try:
            conn = psycopg2.connect(user='root', database='event_detection')
            conn.autocommit = True
        except:
            print("Error: cannot connect to event_detection database")
            sys.exit()
        try:
            self.cursor = conn.cursor()
        except:
            print("Error: cannot create cursor")
            sys.exit()

    def get_unprocessed_queries(self):
        self.cursor.execute("SELECT q.id, q.subject, q.verb, q.direct_obj, q.indirect_obj, q.loc \
                             FROM queries q \
                             WHERE q.processed = false")
        return self.cursor.fetchall()


    def get_unprocessed_query_article_pairs(self):
        self.cursor.execute("SELECT qa.query, qa.article FROM query_articles qa\
                            WHERE qa.processed = false")
        return self.cursor.fetchall()

    def get_query_synonyms(self, query_id):
        self.cursor.execute("SELECT word, pos, sense, synonyms FROM query_words WHERE query=%s", (query_id,))
        return self.cursor.fetchall()
	
    def get_article_keywords(self, article_id):
        self.cursor.execute("SELECT keywords FROM articles WHERE id=%s", (article_id, ))
        keywords = []
        for wrd in self.cursor.fetchall():
            keywords.append(wrd)
        return keywords

    def get_articles(self):
        self.cursor.execute("SELECT id FROM articles")
        return self.cursor.fetchall()

    def insert_article_keywords(self, article_title, source, url, filename, keyword_list):
        keywords = []
        for pos in keyword_list:
            for keyword in keyword_list[pos]:
                keywords.append(keyword + "_" + pos)
        self.cursor.execute("INSERT INTO articles (title, source, url, filename, keywords) VALUES (%s, %s, %s, %s, %s)", (article_title, source, url, filename, keywords))
        return

    def insert_query(self, userid, subject, verb, direct_obj, indirect_obj, loc):
        """Inserts query into 'queries' table and returns the assigned query id"""
        self.cursor.execute("INSERT INTO queries (userid, subject, verb, direct_obj, indirect_obj, loc) VALUES (%s, %s, %s, %s, %s, %s)",
                                                 (userid, subject, verb, direct_obj, indirect_obj, loc))
        return self.get_query_id(userid, subject, verb, direct_obj, indirect_obj, loc)

    def get_query_id(self, userid, subject, verb, direct_obj, indirect_obj, loc):
        self.cursor.execute("""SELECT id FROM queries WHERE userid=%s AND subject=%s AND verb=%s 
                                                        AND direct_obj=%s AND indirect_obj=%s AND loc=%s""",
                                                        (userid, subject, verb, direct_obj, indirect_obj, loc))
        return self.cursor.fetchone()

    def insert_query_word_synonym(self, query_id, query_word, pos_group, synonyms):
        self.cursor.execute("INSERT  INTO query_words (query, word, pos, sense, synonyms) VALUES (%s, %s ,%s, 'Random',%s)", \
                                    (query_id, query_word, pos_group, synonyms))

    def post_validator_update(self, matching_prob, query_id, article_id):
        self.cursor.execute("UPDATE article_queries SET processed=true, accuracy=%s WHERE query=%s AND article=%s",\
                           (matching_prob, query_id, article_id))

    def post_query_processor_update(self, query_id):
        self.cursor.execute("UPDATE queries SET processed=true WHERE id=%s", (query_id, ))
        for article_id in self.get_articles():
            self.cursor.execute("INSERT  INTO query_articles (query, article) VALUES (%s, %s)", (query_id, article_id))

    def insert_user(self, user_name, phone, email):
        """Inserts user into 'users' table and returns the assigned user id"""
        self.cursor.execute("INSERT INTO users (user_name, phone, email) VALUES (%s, %s, %s)",
                                               (user_name, phone, email))
        return self.get_user_id(user_name, phone, email)

    def get_user_id(self, user_name, phone, email):
        self.cursor.execute("SELECT id FROM users WHERE user_name=%s AND phone=%s AND email=%s", (user_name, phone, email))
        return self.cursor.fetchone()

    def get_email_and_phone(self, query_id):
        self.cursor.execute("SELECT userid FROM queries WHERE id = "+str(query_id))
        user_id = self.cursor.fetchone()
        self.cursor.execute("SELECT phone FROM users WHERE id = "+str(user_id))
        phone = str(self.cursor.fetchone())
        if phone != None:
            phone = re.sub(r'-', '', phone)
            phone = "+1" + phone
        self.cursor.execute("SELECT email FROM users WHERE id = "+str(user_id))
        email = str(self.cursor.fetchone())
        return phone, email


    def user_status(self, user_name, phone, email):
        """Takes in a username and returns 0 if username is already taken with different phone/email, 
        1 if username is repeat, 2 if user is new"""
        self.cursor.execute("SELECT id FROM users WHERE user_name=%s", (user_name, ))
        username_exists = (self.cursor.fetchone() != None)
        self.cursor.execute("SELECT id FROM users WHERE user_name=%s AND phone=%s AND email=%s", (user_name, phone, email))
        identical_user_exists = (self.cursor.fetchone() != None)
        if username_exists and not identical_user_exists: # Username already has different phone/email assigned
            return 0
        elif username_exists: # Duplicate user
            return 1
        else: # New user
            return 2

#def main():
#    pass

# if __name__ == '__main__':
#     main()