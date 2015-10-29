import sys
from Validator import *
from nltk import pos_tag, word_tokenize

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

        query = Query(1, queryParts, threshold)

        articlePool = [Article("Article 1 about Beyonce", "Beyonce song", "url", "source"), \
                       Article("Article 2 about Beyonce", "Beyonce song", "url", "source"), \
                       Article("Article 3 about Race", "Race song", "url", "source")]
        


        print("RESULT:\nArticles that matched:")
        numMatchingArticle = 0
        for article in articlePool:
            keywordValidator = KeywordValidator()
            matchPercentage = keywordValidator.validate(query, article)
            if matchPercentage > 0.4:
                numMatchingArticle += 1
                print(article.getTitle())
        if numMatchingArticle == 0:
            print("No matching articles")

    else:
        print("Please enter exactly 5 elements.")






if __name__ == "__main__":
    main()