#!/usr/bin/env bash
brew tap 'toberumono/tap'
brew install 'toberumono/tap/utils' 'toberumono/tap/structures' 'toberumono/tap/lexer' 'toberumono/tap/json-library'
brew install 'postgresql' '--devel'
echo '------------------Setting Up PostgreSQL Database---------------------'
sudo gem install lunchy
initdb /usr/local/var/postgres
lunchy start postgresql
lunchy stop postgresql
createdb event_detection
lunchy start postgresql
psql event_detection < setup.sql
psql event_detection < seeds.sql
echo '------------------Generating Eclipse User Libraries------------------'
. "./generate_userlibs.sh"