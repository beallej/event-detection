from bs4 import BeautifulSoup
import urllib.request

def read(page):
    """
    Reads the main content from a Reuters page
    """
    soup = BeautifulSoup(page, "html.parser")
    text = soup.find(id = "articleText")
    return " ".join(text.strings)

def main():
    page = urllib.request.urlopen("http://www.reuters.com/article/us-usa-election-democrats-idUSKCN0UV058?feedType=RSS&feedName=topNews")
    read(page)

if __name__ == "__main__":
    main()
