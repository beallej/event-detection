import RAKEtutorialmaster.rake as rake



def main():
    rake_object = rake.Rake("RAKEtutorialmaster/SmartStoplist.txt", 4, 3, 3)
    sample_file = open("nicki.txt", 'r')
    text = sample_file.read()
    keywords = rake_object.run(text)
    print("Keywords:", keywords)

    keywords = rake_object.run_stemmed(text)
    print("Keywords Stemmed:", keywords)



main()

