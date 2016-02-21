# Event Detection
### Comps 2016
#### Carleton College
##### Josie Bealle, Laura Biester, Phuong Dinh, Julia Kroll, Josh Lipstone, and Anmol Raina

#### Installations
1. [Install Homebrew](http://brew.sh/)
1. Downloader (Run all commands from the root directory of the repository):
  1. `./setup_project.sh` - Handles psycopg2, Flask, and PostgreSQL
  2. `ant -Dprefix='./'`
6. Auto-run: `./install_crontab.sh`

#### Running the Downloader and Validator
1. Just do it. `./run.sh`

#### Running the Web App
1. Run the application: `python3 WebApp/EventDetectionWeb.py`
2. To view the application, navigate to [localhost:5000](http://localhost:5000/)

#### After Pulling New Code
1. Update Brew: `brew update && brew upgrade`
2. Run Ant: `ant -Dprefix='./'`
