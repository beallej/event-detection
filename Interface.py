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
        queryParts = {"query": ' '.join(userInput), "subject": userInput[0], "verb": userInput[1], "direct_obj": userInput[2], \
        "indirect_obj": userInput[3], "location" : userInput[4]}

        # put into database -> get id
        threshold = None

        query = Query(1, queryParts, threshold)
        sample_file = open("test.txt", 'r')
        text = sample_file.read()

        sample_file3 = open("articles/13_CNN_Tiger_NNP_bites_VBZ_woman_NN_in_IN_Halloween_NNP_zoo_NN_trespass_NN.txt", 'r')
        text3 = sample_file3.read()
        articlePool = [Article("Tiger_NNP bites_VBZ woman_NN in_IN Halloween_NNP zoo_NN trespass_NN.txt", text3, "url", "source")]
        for article in articlePool:
            print(article.keyword)
        


        print("RESULT:\nArticles that matched:")
        numMatchingArticle = 0
        for article in articlePool:
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