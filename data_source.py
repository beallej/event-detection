#!/usr/bin/python

# import cgitb
# cgitb.enable()

import psycopg2

class DataSource:
    
    def __init__(self):
        try:
            conn = psycopg2.connect(user='root', database='event_detection')
        except:
            sys.exit()
        try:
            self.cursor = conn.cursor()
        except:
            sys.exit()
            
    def get_keywords(self, article_title):
        #try:
        self.cursor.execute("SELECT (keywords) FROM articles WHERE title=%s;", (article_title, ))
        keywords = []
        for keyword in self.cursor:
                keywords.append(keyword)
        #except:
        #    print("Error: cannot get keywords")
        return keywords

    def insert_keywords(self, article_title, keyword_list):
       # try:
        keyword_string = "{"
        for wrd in keyword_list:
            formatted_wrd = '"' + wrd + '" ,'
            keyword_string += formatted_wrd
        keyword_string = keyword_string[:-1]
        keyword_string += "}"
        self.cursor.execute("INSERT INTO articles (title, keywords) VALUES (%s, %s);", (article_title, keyword_string, ))
        #except:
        #    print("Error: cannot insert keywords")
        return

    # def get_sources(self):
    #     try:
    #         self.cursor.execute("SELECT * FROM sources;")
    #         source_data = []
    #         for source in self.cursor:
    #             for i in range(len(source)):
    #                 source_data.append(source[i])
    #     except:
    #         print("Error: cannot get sources")
    #     return source_data

    # def get_feeds(self):
    #     try:
    #         self.cursor.execute("SELECT * FROM feeds;")
    #         feed_data = []
    #         for feed in self.cursor:
    #             for i in range(len(feed)):
    #                 feed_data.append(feed[i])
    #     except:
    #         print("Error: cannot get feeds")
    #     return feed_data
    
    # def get_articles(self):
    #     try:
    #         self.cursor.execute("SELECT * FROM articles;")
    #         article_data = []
    #         for article in self.cursor:
    #             for i in range(len(article)):
    #                 article_data.append(article[i])
    #     except:
    #         print("Error: cannot get articles")
    #     return article_data

    # def add_test_article(self):
    #     try:
    #         self.cursor.execute("INSERT INTO articles (title, url) VALUES ('Test Article Arrives', 'www.testing-now.com');")
    #     except:
    #         print("Error: cannot add article")
    #     return

    # def remove_test_article(self):
    #     try:
    #         self.cursor.execute("DELETE FROM articles WHERE title='Test Article Arrives';")
    #     except:
    #         print("Error: cannot remove article")
    #     return

def main():
    ds = ValidatorDataSource()
    print("SOURCES:\n", ds.get_sources(), "\n")
    print("FEEDS:\n", ds.get_feeds(), "\n")
    print("ARTICLES:\n", ds.get_articles(), "\n")
    ds.add_test_article()
    print("ARTICLES after added:\n", ds.get_articles(), "\n")
    ds.remove_test_article()
    print("ARTICLES after removed:\n", ds.get_articles(), "\n")
    print("done.")

# if __name__ == '__main__':
#     main()