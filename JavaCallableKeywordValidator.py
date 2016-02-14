from Validator import KeywordValidator
import sys

if __name__ == "__main__":
    print(KeywordValidator().validate(int(sys.argv[1]), int(sys.argv[2])))
