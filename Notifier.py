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
    sendgrid_api_user = "event-detection"
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
        self.email_client = sendgrid.SendGridClient(self.sendgrid_api_user, self.sendgrid_api_key)

    def alert(self, message):
        """
        Alerts user
        :param message: the email/text body
        :return: None
        """
        def alert_phone():
            """
            Sends text message
            :return: None
            """
            if self.phone != None:
                self.phone_client.messages.create(body=message, to=self.phone_test, from_=self.twilio_number)
        def alert_email():
            """
            Sends email message
            :return: None
            """
            if self.email != None:
                message = sendgrid.Mail()
                message.add_to(self.email_test)

                #not sure what address goes here-- maybe test email?
                message.set_from()
                message.set_subject("Event Detection")

                #unclear which one to use
                message.set_html(" test1")
                message.set_text("test2")

                status, msg = self.email_client.send(message)

        alert_phone()
        alert_email()