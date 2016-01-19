from bs4 import BeautifulSoup
import sys
import urllib.request

def read():
    """
    Reads the main content from a Reuters page
    """
    url = input()
    page = urllib.request.urlopen(url)
    soup = BeautifulSoup(page, "html.parser")
    text = soup.find(id = "articleText")
    print(" ".join(text.strings))

def main():
    read()

if __name__ == "__main__":
    main()
