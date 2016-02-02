import json
import sys

"""
Holds global information, some of which may be changed at runtime
"""

file = open('version.json')
json_object = json.loads(file.read())
articles_path = json_object["articles"]
database = json_object["db"]
file.close()


def main():
    """
    supply arg "test" for test setup
    default reuglar setup
    :return: None
    """
    myfile = open('version.json')
    json_object = json.loads(myfile.read())
    myfile.close()
    if len(sys.argv) == 1:
        json_object["articles"] = "articles/"
        json_object["db"] = "event_detection"
    elif sys.argv[1] == "test":
        json_object["articles"] = "articles_test/"
        json_object["db"] = "event_detection_test"
    myfile = open("version.json", 'w')
    json.dump(json_object, myfile)
    myfile.close()

if __name__ == "__main__":
    main()