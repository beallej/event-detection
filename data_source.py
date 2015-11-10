#!/usr/bin/python

# import cgitb
# cgitb.enable()

import psycopg2
import sys

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
            
    def get_article_keywords(self, article_title):
        #try:
        self.cursor.execute("SELECT keywords FROM articles WHERE title=%s", (article_title, ))
        #self.cursor.execute("SELECT keywords FROM articles")
        return self.cursor.fetchall()

    def get_articles(self):
        self.cursor.execute("SELECT * FROM articles")
        #for article in self.cursor:
         #   print(article)
        return self.cursor.fetchall()

    def insert_article_keywords(self, article_title, keyword_list):
       # try:
        keyword_string = "{"
        for wrd in keyword_list:
            formatted_wrd = '"' + wrd + '" ,'
            keyword_string += formatted_wrd
        keyword_string = keyword_string[:-1]
        keyword_string += "}"
        self.cursor.execute("INSERT INTO articles (title, keywords) VALUES (%s, %s)", (article_title, keyword_string, ))
        #except:
        #    print("Error: cannot insert keywords")
        return

    def insert_query(self, userid, subject, verb, direct_obj, indirect_obj, loc):
        self.cursor.execute("INSERT INTO queries (userid, subject, verb, direct_obj, indirect_obj, loc) VALUES (%s, %s, %s, %s, %s, %s)",
                                                 (userid, subject, verb, direct_obj, indirect_obj, loc, ))
        return

    #HERE  def get_query_id(self, subject, verb, direct_obj, indirect_obj, loc):
     #   self.cursor.execute

    # Inserts user into 'users' table and returns the assigned user id
    def insert_user(self, user_name, phone, email):
        self.cursor.execute("INSERT INTO users (user_name, phone, email) VALUES (%s, %s, %s)",
                                               (user_name, phone, email, ))
        return self.get_user_id(user_name, phone, email)

    def get_user_id(self, user_name, phone, email):
        self.cursor.execute("SELECT (id) FROM users WHERE user_name=%s AND phone=%s AND email=%s", (user_name, phone, email, ))
        return self.cursor.fetchone()

#def main():
#    pass

# if __name__ == '__main__':
#     main()