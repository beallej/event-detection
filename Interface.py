import sys
from Validator import *
from Notifier import *
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
        queryParts = {"query": ' '.join(userInput), "subject": userInput[0], "verb": userInput[1], "direct_obj": userInput[2], \
        "indirect_obj": userInput[3], "location" : userInput[4]}

        # put into database -> get id
        threshold = None

        query = Query(1, queryParts, threshold)

        sample_file = open("articles/9_1_Police___9-year-old_boy_lured_into_alley_,_shot.txt", 'r')
        text = sample_file.read()
        articlePool = [Article("Title", text, "www.carleton.edu", "source")]
        for article in articlePool:
            article.extract_keyword()
            print(article.keyword)

        notifier = Notifier(Notifier.email_test, Notifier.phone_test)

        print("RESULT:\nArticles that matched:")
        numMatchingArticle = 0
        for article in articlePool:
            keywordValidator = KeywordValidator()
            matchPercentage = keywordValidator.validate(query, article)
            if matchPercentage > 0.2:
                numMatchingArticle += 1
                notifier.on_validation(query, article)
                print(article.get_title())
        if numMatchingArticle == 0:
            print("No matching articles")

    else:
        print("Please enter exactly 5 elements.")






if __name__ == "__main__":
    main()