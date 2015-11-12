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

        all_articles = "10_1_Louisiana_boy's_slaying_reveals_troubles_,_power_struggles.txt \
11_1_Judge_sentences_Jewish_center_killer_to_death.txt \
12_1_Look_at_me_!_25_selfies_of_the_week.txt \
13_1_Photographer_shows_wounded_vets_in_new_light.txt \
14_1_Texas_biker_shootout___106_indicted_by_grand_jury.txt \
15_1_3_accused_in_plot_to_start_race_war.txt \
16_1_Plane_hits_house_;_deaths_reported.txt \
17_1_Military_kids_salute_their_parents.txt \
18_1_Supporting_military_families_and_kids.txt \
19_1_Scare_at_Miami_airport.txt \
1_1_Boy_,_8_charged_with_murder_of_girl_,_1_,_while_parents_went_clubbing.txt \
20_1_A_timeline_of_the_University_of_Missouri_protests.txt \
21_1_When_athletes_unite_,_the_powerful_listen.txt \
22_1_University_of_Missouri_campus_protests___`_This_is_just_a_beginning_'.txt \
23_1_Do_U.S._colleges_have_a_race_problem_?.txt \
24_1_Community_in_shock_over_killing_of_9-year-old_Tyshawn_Lee.txt \
25_1_Source___Officer_charged_in_death_knew_boy's_father.txt \
2_1_Rumors_and_racial_threats_stoke_fear_at_Mizzou.txt \
3_1_Decades_of_goo_stripped_off_gum_wall_at_Pike_Place_Market.txt \
4_1_Shock_,_horror_after_plane_hits_apartment_building.txt \
5_1_Einstein's_colossal_mistake.txt \
6_1_Americans_observe_Veterans_Day.txt \
7_1_When_people_die_in_strange_ways.txt \
8_1_How_to_end_Chicago's_cycle_of_violence.txt \
9_1_Removing_hijab_,_finding_myself.txt"
        for article in all_articles.split(" "):

            sample_file = open("articles/"+article, 'r')
            text = sample_file.read()
            articlePool = [Article("Shock, horror after plane hits apartment building", text, "www.carleton.edu", "source")]
            for article in articlePool:
                article.extract_keyword()
                print(article.keyword)
            print("-----------")

        notifier = Notifier()

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