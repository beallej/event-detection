# Event Detection
### Comps 2016
#### Carleton College
##### Josie Bealle, Laura Biester, Phuong Dinh, Julia Kroll, Josh Lipstone, and Anmol Raina

#### Installations
1. [Install Homebrew](http://brew.sh/)
1. Downloader (Run all commands from the root directory of the repository):
  1. `./Setup_Teardown/setup_project.sh` - Handles psycopg2, Flask, and PostgreSQL. You may be asked to [download the JDK] (http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
    1. Note: If postgres failes to create the database (this sometimes happens if the createdb command is executed while postgres is still starting up), run `./Setup_Teardown/setup_project.sh -s true`.
  2. `ant -Dprefix='./'`
6. Auto-run: `./Setup_Teardown/install_crontab.sh`

#### Registrations
Create a file in the Utils folder named `Secrets.py`, with the following text:
```
import sys; import os
sys.path.insert(0, os.path.abspath('..'))
sys.path.insert(0, os.path.abspath('.'))
​
from_email = ""
twilio_number = "" #use this format, ex. "+19999999999"
twilio_account_sid = ""
twilio_auth_token = ""
sendgrid_api_key = ""
bitly_api_login = ""
bitly_api_key = ""
```
To fill in empty strings:
  1. Sign up for Twilio. Account SID and Auth Token can be found here: https://www.twilio.com/user/account/settings. Phone number can be found here: https://www.twilio.com/user/account/phone-numbers/incoming.
  2. Sign up for SendGrid (they have to approve you, so it may take a while). After approval, go here https://app.sendgrid.com/settings/api_keys, generate an api key, and paste it in (the long secret version that is only displayed when you generate it).
  3. Sign up for bitly. Go here https://app.bitly.com/bitlinks/?actions=accountMain. In the sidebar that pops up, click Advanced Settings -> API Support, and copy and paste in the api login and key displayed.
  4. Input any email address to send from.

#### Running the Downloader, Validator and Pipeline
1. Just do it. `java -jar pipeline.jar`

#### Running the Web App
1. Run the application: `python3 WebApp/EventDetectionWeb.py`
2. To view the application, navigate to [localhost:5000](http://localhost:5000/)

#### After Pulling New Code
1. Update Brew: `brew update && brew upgrade`
2. Run Ant: `ant -Dprefix='./'`

#### Testing
1. Navigate to the Testing directory.
1. Unzip `articles_test` folder. Alternatively, if using your own testing data, you can create your own folder of test articles labeled `articles_test`.
2. Run `setup_project_test.sh`
3. To use our`psql event_detection_test < test_database.sql`. Do not do this if using your own testing data, as test_backup is a dump of our testing data.
4. Various testing functions can be found in `Testing/Tester.py`. Make sure you run all programs from root project directory.
