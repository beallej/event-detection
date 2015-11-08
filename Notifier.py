from twilio.rest import TwilioRestClient
import smtplib
from email.mime.text import MIMEText

class Notifier:
    def __init__(self, email, phone):
        self.email = email
        self.phone = phone

        self.phone = "+15132952362"

        # Your Account Sid and Auth Token from twilio.com/user/account
        account_sid = "AC7b50b072cd7cc54e912eb28dffd3c403"
        auth_token  = "3b8e4111c3d10fdeffc666fddd65e6a3"
        self.client = TwilioRestClient(account_sid, auth_token)

    def alert(self, message):
        def alert_phone():
            if self.phone != None:
                to_send = self.client.messages.create(body=message,
                    to=self.phone,
                    from_="+15137269006") # Twilio number

        def alert_email():
            pass
            # # me == the sender's email address
            # # you == the recipient's email address
            # msg['Subject'] = "Event Notification"
            # msg['From'] = me
            # msg['To'] = you
            #
            # # Send the message via our own SMTP server, but don't include the
            # # envelope header.
            # s = smtplib.SMTP('localhost')
            # s.sendmail(me, [you], msg.as_string())
            # s.quit()

        alert_phone()
        alert_email()