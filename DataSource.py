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
        words = self.cursor.fetchone()
        return words

    def get_all_article_keywords(self):
        self.cursor.execute("SELECT keywords FROM articles WHERE keywords IS NOT null;")
        return self.cursor.fetchall()

    def get_all_titles_and_keywords(self):
        self.cursor.execute("SELECT title, keywords FROM articles WHERE keywords IS NOT null;")
        return self.cursor.fetchall()

    def get_all_article_ids_and_keywords(self):
        self.cursor.execute("SELECT id, keywords FROM articles WHERE keywords IS NOT null;")
        return self.cursor.fetchall()

    def get_articles(self):
        self.cursor.execute("SELECT id FROM articles")
        return self.cursor.fetchall()

    def get_all_article_ids_and_filenames(self):
        self.cursor.execute("SELECT id, filename FROM articles;")
        return self.cursor.fetchall()

    def get_article_ids_titles_filenames(self):
        self.cursor.execute("SELECT id, title, filename FROM articles;")
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
        self.cursor.execute("INSERT INTO query_words (query, word, pos, sense, synonyms) VALUES (%s, %s ,%s, '',%s)", \
                                    (query_id, query_word, pos_group, synonyms))

    def post_validator_update(self, matching_prob, query_id, article_id):
        self.cursor.execute("UPDATE query_articles SET processed=true, accuracy=%s WHERE query=%s AND article=%s",\
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

    def get_query_elements(self, query_id):
        self.cursor.execute("SELECT subject, verb, direct_obj, indirect_obj, loc FROM queries WHERE id=%s", (query_id, ))
        elements = self.cursor.fetchone()
        elements = [element for element in elements if element is not None or element is not ""]
        return elements

    def get_article_url(self, article_id):
        self.cursor.execute("SELECT url FROM articles WHERE id=%s", (article_id, ))
        return str(self.cursor.fetchone()[0])

    def get_article_title(self, article_id):
        self.cursor.execute("SELECT title FROM articles WHERE id=%s", (article_id, ))
        return str(self.cursor.fetchone()[0])

    def get_email_and_phone(self, query_id):
        self.cursor.execute("SELECT userid FROM queries WHERE id="+str(query_id))
        user_id = self.cursor.fetchone()[0]
        self.cursor.execute("SELECT phone FROM users WHERE id="+str(user_id))
        phone = str(self.cursor.fetchone()[0])
        if phone is not None:
            phone = re.sub(r'-', '', phone)
            phone = "+1" + phone
        self.cursor.execute("SELECT email FROM users WHERE id="+str(user_id))
        email = str(self.cursor.fetchone()[0])
        return phone, email

    def user_status(self, user_name, phone, email):
        """Takes in a username and returns 0 if username is already taken with different phone/email,
        1 if username is repeat, 2 if user is new"""
        self.cursor.execute("SELECT id FROM users WHERE user_name=%s", (user_name, ))
        username_exists = (self.cursor.fetchone() is not None)
        self.cursor.execute("SELECT id FROM users WHERE user_name=%s AND phone=%s AND email=%s", (user_name, phone, email))
        identical_user_exists = (self.cursor.fetchone() is not None)
        if username_exists and not identical_user_exists: # Username already has different phone/email assigned
            return 0
        elif username_exists:  # Duplicate user
            return 1
        else:  # New user
            return 2

    def get_unprocessed_articles(self):
        self.cursor.execute("SELECT id, title, filename, url, source FROM articles WHERE keywords is null;")
        return self.cursor.fetchall()

    def add_keywords_to_article(self, id, keyword_string):
        self.cursor.execute("UPDATE articles SET keywords = %s WHERE id = %s", (keyword_string, id))

    def set_all_unprocessed(self):
        self.cursor.execute("UPDATE articles SET keywords = NULL")
        self.cursor.execute("UPDATE query_articles SET processed = false")
        self.cursor.execute("UPDATE query_articles SET accuracy = 0")

    def get_all_ids_and_titles(self):
        self.cursor.execute("SELECT id, title FROM articles;")
        return self.cursor.fetchall()

    def article_processed(self, article_id):
        self.cursor.execute("SELECT keywords FROM articles WHERE id = %s;", (article_id, ))
        return self.cursor.fetchone()[0] is not None

    def query_route(self, query_id):
        """
        Gets a query for web app with validating article counts
        :param query_id: the id of the query
        :return: the query
        """
        self.cursor.execute("SELECT a.title, s.source_name as source, a.url \
                    FROM queries q \
                    INNER JOIN query_articles qa on q.id = qa.query \
                    INNER JOIN articles a on qa.article = a.id \
                    INNER JOIN sources s on s.id = a.source \
                    WHERE q.id = %s and qa.notification_sent = true;", (query_id,))
        articles = self.cursor.fetchall()

        self.cursor.execute("SELECT id, subject, verb, direct_obj, indirect_obj, loc FROM queries where id = %s;", (query_id,))
        query = self.cursor.fetchone()
        return articles, query

    def queries_route(self):
        """
        Gets all queries for web app with validating article counts
        :return: all queries
        """
        self.cursor.execute("SELECT q.id, q.subject, q.verb, q.direct_obj, q.indirect_obj, \
                           q.loc, count(qa.article) as article_count \
                    FROM queries q \
                    LEFT JOIN query_articles qa on q.id = qa.query and qa.notification_sent = true \
                    GROUP BY(q.id);")
        return self.cursor.fetchall()

    def new_query(self, email, phone, subject, verb, direct_obj, indirect_obj, loc):
        """
        Add a query to the database for a user
        :param email: the user's email
        :param phone: the user's phone number
        :param subject: the query subject
        :param verb: the query verb
        :param direct_obj: the query direct object
        :param indirect_obj: the query indirect object
        :param loc: the query location
        :return: True if no error
        """
        self.cursor.execute("SELECT id from users where email = %s and phone = %s", (email, phone))
        # use existing user if it exists
        user_id = self.cursor.fetchone()
        if user_id:
            user_id = user_id[0]
        else:
            self.cursor.execute("INSERT INTO users (email, phone) VALUES (%s, %s) RETURNING id;", (email, phone))
            user_id = self.cursor.fetchone()[0]

        try:
            self.cursor.execute("INSERT INTO queries (subject, verb, direct_obj, indirect_obj, loc, userid) \
                            VALUES (%s, %s, %s, %s, %s, %s);", (subject, verb, direct_obj, indirect_obj, loc, user_id))
        except psycopg2.IntegrityError:
            return False
        return True

    def articles_route(self):
        self.cursor.execute("SELECT title, s.source_name as source, url FROM articles a \
                        INNER JOIN sources s on s.id = a.source;")
        return self.cursor.fetchall()
