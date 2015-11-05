#!/usr/bin/env bash
brew tap 'toberumono/tap'
brew install 'toberumono/tap/utils' 'toberumono/tap/structures' 'toberumono/tap/lexer' 'toberumono/tap/json-library' 'wget'
brew install 'postgresql' '--devel'
echo '----------------Downloading Downloader Dependencies-------------------'
wget '-N' '--directory-prefix=../' 'http://central.maven.org/maven2/com/rometools/rome-utils/1.5.1/rome-utils-1.5.1.jar'
wget '-N' '--directory-prefix=../' 'http://central.maven.org/maven2/com/rometools/rome/1.5.1/rome-1.5.1.jar'
wget '-N' '--directory-prefix=../' 'http://central.maven.org/maven2/org/jdom/jdom2/2.0.6/jdom2-2.0.6.jar'
wget '-N' '--directory-prefix=../' 'http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.12/slf4j-api-1.7.12.jar'
wget '-N' '--directory-prefix=../' 'http://central.maven.org/maven2/org/slf4j/slf4j-simple/1.7.12/slf4j-simple-1.7.12.jar'
wget '-N' '--directory-prefix=../' 'https://jdbc.postgresql.org/download/postgresql-9.4-1205.jdbc42.jar'
wget '-N' '--directory-prefix=../' 'http://nlp.stanford.edu/software/stanford-corenlp-full-2015-04-20.zip'
unzip '../stanford-corenlp-full-2015-04-20.zip'
echo '------------------Setting Up PostgreSQL Database---------------------'
sudo gem install lunchy
initdb /usr/local/var/postgres
lunchy start postgresql
createdb event_detection
psql event_detection < setup.sql
psql event_detection < seeds.sql
echo '------------------Generating Eclipse User Libraries------------------'
. "./generate_userlibs.sh"