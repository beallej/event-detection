import sys
from Validator import *
from nltk import pos_tag, word_tokenize
from data_source import DataSource

ds = DataSource()

def main():
    userInput = input("Please enter query, in the form of :Subject/Verb/Direct Object/Indirect Object/\
Location\nDirect Object, Indirect Object, Location are Optional.If you don't supply them make \
sure you still have 4 slashes.\n")
    userInput = userInput.split("/")
    if len(userInput) == 5:
        if userInput[0] == "" or userInput[1] == "" :
            print("Subject and Verb are required!")
            return
        queryParts = {"query": ' '.join(userInput), "subject": userInput[0], "verb": userInput[1], "directObj": userInput[2], \
        "indirectObj": userInput[3], "location" : userInput[4]}

        # put into database -> get id
        threshold = None

        #print("testing database now by getting articles")
        #print(ds.get_article_keywords("The Article Title"))



        query = Query(queryParts, threshold)
        query.store()
        sample_file = open("cancer.txt", 'r')
        text = sample_file.read()

        sample_file3 = open("ukraine.txt", 'r')
        text3 = sample_file3.read()
        articlePool = [Article("Food experts agree: WHO report on cancer and meat is spot on", text, "url", "source"),\
                       Article("Fraud Claims Delay Elections in Two Ukrainian Cities", text3, "url", "source")]
        for article in articlePool:
            print("Keywords: ", article.getKeywords())
        


        print("RESULT:\nArticles that matched:")
        numMatchingArticle = 0
        for article in articlePool:
            print(article.getKeywords())
            keywordValidator = KeywordValidator()
            matchPercentage = keywordValidator.validate(query, article)
            if matchPercentage > 0.2:
                numMatchingArticle += 1
                print(article.getTitle())
        if numMatchingArticle == 0:
            print("No matching articles")

    else:
        print("Please enter exactly 5 elements.")






if __name__ == "__main__":
    main()