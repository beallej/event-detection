import sys; import os
sys.path.insert(0, os.path.abspath('..'))
sys.path.insert(0, os.path.abspath('.'))

from twilio.rest import TwilioRestClient
import sendgrid, json
from Utils.DataSource import *
import requests


class Notifier:
    """
    Used to notify user of query detection
    """

    # TODO: MOVE THIS STUFF TO A MORE SECURE LOCATION
    default_phone = "+15073385228"
    default_email = "event.detection.carleton@gmail.com"
    twilio_number = "+15137269006"
    twilio_account_sid = "AC7b50b072cd7cc54e912eb28dffd3c403"
    twilio_auth_token = "3b8e4111c3d10fdeffc666fddd65e6a3"
    sendgrid_api_key = "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"
    bitly_api_login = "o_3s58or5kei"
    bitly_api_key = "R_e8af4fe78cc54bcf869837cb2ff1c501"
    bitly_api_url = "https://api-ssl.bitly.com"

    def __init__(self):
        """
        Initializes notification clients.
        :return: None
        """
        self.datasource = DataSource()
        self.phone_client = TwilioRestClient(self.twilio_account_sid, self.twilio_auth_token)
        self.email_client = sendgrid.SendGridClient(self.sendgrid_api_key)

    def alert_phone(self, text):
        """
        Sends text message
        :param message: the text body
        :return: None
        """
        if self.phone != None and self.phone != "+1":
            self.phone_client.messages.create(body=text, to=self.phone, from_=self.twilio_number)

    def alert_email(self, text):
        """
        Sends email message
        :param text: the email body in html
        :return: None
        """
        if self.email != None:
            message = sendgrid.Mail()
            message.add_to(self.email)

            message.set_from(self.default_email)
            message.set_subject("Event Detection")
            message.set_html(text)

            status_code, status_message = self.email_client.send(message)
            if int(status_code) != 200:
                print("Error " + str(status_code) + " : " + str(status_message))

    def on_validation(self, query_id, article_ids):
        """
        Notifies user on validation
        :param query_id: query that was validated
        :param article_ids: articles that validated query
        :return: None
        """
        query_string = " ".join(self.datasource.get_query_elements(query_id))
        article_data = []
        for article_id in article_ids:
            article_url = self.get_article_shortlink(self.datasource.get_article_url(article_id))
            article_title = self.datasource.get_article_title(article_id)
            article_data.append((article_title, article_url))

        html = self.format_html(query_string, article_data)
        texts = self.format_plaintext(query_string, article_data)

        self.phone, self.email = self.datasource.get_email_and_phone(query_id)
        self.alert_email(html)
        for text in texts:
            self.alert_phone(text)

    @staticmethod
    def format_html(query_string, article_data):
        """
        formats body of email
        :param query_string: query that was validated
        :param article_data: article that validated query
        :return: html of email body
        """
        html = "<h1>{query}</h1><p>Articles:</p>".format(query = query_string)
        for article in article_data:
            article_title = article[0]
            article_url = article[1]
            html += "<p><a href=\"{url}\">{title}</a></p>".format(url=article_url, title=article_title)
        return html

    @staticmethod
    def format_plaintext(query_string, article_data):
        """
        formats text message
        formats body of email
        :param query_string: query that was validated
        :param article_data: article that validated query
        :return: text body
        """
        texts = []
        text = "Event Detected!\nQuery: {query}\nArticles: ".format(query = query_string)
        for article in article_data:
            article_title = article[0]
            article_url = article[1]
            next_article = "\n{title}\nLink {url}\n".format(url=article_url, title=article_title)
            if len(text) + len(next_article) > 1600:
                texts.append(text)
                text = "Event Detected!\nQuery: {query}\nArticles: ".format(query = query_string)
            text += next_article
        texts.append(text)
        return texts

    def get_article_shortlink(self, article_url):
        """
        Gets a shortlink from bitly for the article url
        :param article_url: the url to shorten
        :return: the shortened url if successful (otherwise just the article url)
        """
        payload = {"longUrl": article_url, "login": self.bitly_api_login, "apiKey": self.bitly_api_key}
        response = requests.get(self.bitly_api_url + "/v3/shorten", params=payload)
        response_json = response.json()
        # look for data -> url -> short url in response_json
        # if it's not there, just return the old url
        if "data" in response_json and "url" in response_json["data"]:
            return response_json["data"]["url"]
        return article_url


def main():
    notifier = Notifier()
    json_text = ""
    with sys.stdin as fileIn:
        for line in fileIn:
            json_text = json_text + line
    json_obj = json.loads(json_text)

    for query, articles in json_obj.items():
        notifier.on_validation(int(query), articles)

if __name__ == "__main__":
    main()
