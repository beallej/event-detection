from twilio.rest import TwilioRestClient
import sendgrid

class Notifier:
    """
    Used to notify user of query detection
    """

    #TODO: MOVE THIS STUFF TO A MORE SECURE LOCATION
    phone_test = "+15073385228"
    email_test = "event.detection.carleton@gmail.com"
    twilio_number = "+15137269006"
    twilio_account_sid = "AC7b50b072cd7cc54e912eb28dffd3c403"
    twilio_auth_token  = "3b8e4111c3d10fdeffc666fddd65e6a3"
    sendgrid_api_key = "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"

    def __init__(self, email, phone):
        """
        Initializes notification clients.
        :param email: Email address to send to, can be None
        :param phone: Phone # to text to, can be None
        :return: None
        """

        self.email = email
        self.phone = phone
        self.phone_client = TwilioRestClient(self.twilio_account_sid, self.twilio_auth_token)
        self.email_client = sendgrid.SendGridClient(self.sendgrid_api_key)


    def alert_phone(self, text):
        """
        Sends text message
        :param message: the text body
        :return: None
        """
        if self.phone != None:
            self.phone_client.messages.create(body=text, to=self.phone_test, from_=self.twilio_number)
    def alert_email(self, text):
        """
        Sends email message
        :param text: the email body in html
        :return: None
        """
        if self.email != None:
            message = sendgrid.Mail()
            message.add_to(self.email_test)

            #not sure what address goes here-- maybe test email?
            message.set_from(self.email_test)
            message.set_subject("Event Detection")
            message.set_html(text)

            status_code, status_message = self.email_client.send(message)
            if int(status_code) != 200:
                print("Error " + str(status_code) + " : " + str(status_message))


    def on_validation(self, query, article):
        """
        Notifies user on validation
        :param query: query that was validated
        :param article: article that validated query
        :return: None
        """
        html = self.format_html(query, article)
        text = self.format_plaintext(query, article)
        self.alert_email(html)
        self.alert_phone(text)

    def format_query(self, query):
        """
        formats query for email or text
        :param query: query to format
        :return: query as string
        """
        query_elements = [query.subject.word, query.verb.word]
        for element in [query.direct_obj.word, query.indirect_obj.word, query.location.word]:
            if element != None and element != "":
                query_elements.append(element)
        return " ".join(query_elements)

    def format_html(self, query, article):
        """
        formats body of email
        :param query: query that was validated
        :param article: article that validated query
        :return: html of email body
        """
        query_string = self.format_query(query)
        html = "<h1>{query}</h1><p>Article: <a href=\"{url}\">{title}</a></p>"\
            .format(query = query_string, url=article.url, title=article.title)
        return html

    def format_plaintext(self, query, article):
        """
        formats text message
        formats body of email
        :param query: query that was validated
        :param article: article that validated query
        :return: text body
        """
        query_string = self.format_query(query)
        text = "Event Detected!\nQuery: {query}\nArticle: {title}\nLink {url}"\
            .format(query = query_string, url=article.url, title=article.title)
        return text