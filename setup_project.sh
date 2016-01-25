#!/usr/bin/env bash
working_dir="$(pwd)"
key_library="postgresql-9.4.1207.jar"

if [ "$(which brew)" != "" ] && [ "$(which brew)" != "brew not found" ]; then
	pip3 install 'psycopg2'
	pip3 install 'Flask'
	pip3 install 'twilio'
	pip3 install 'sendgrid'
	pip3 install 'sklearn'
	brew update
	brew tap 'toberumono/tap'
	brew install 'toberumono/tap/utils' 'toberumono/tap/structures' 'toberumono/tap/lexer' 'toberumono/tap/json-library' 'wget'
	brew install 'postgresql'
else
	cd ../
	git clone "https://github.com/Toberumono/JSON-library.git"
	cd 'JSON-library'
	git checkout "$(git describe --tags)"
	./build_brewless.sh
	cd "$working_dir"
fi

export PGDATA="$(brew --prefix)/var/postgres"
export PGHOST=localhost
[ "$(pg_ctl status | grep 'PID:' )" == "" ] && setup_sql=true || setup_sql=false
[ -e "../${key_library}" ] && download_libs=false || download_libs=true
[ -e "../repackaged-stanford-corenlp.jar" ] && repackage_corenlp=false || repackage_corenlp=true

while getopts s:d:r: opt; do
  case $opt in
  s)
      [ "$OPTARG" == "true" ] && setup_sql=true || setup_sql=false
      ;;
  d)
      [ "$OPTARG" == "true" ] && download_libs=true || download_libs=false
      ;;
  r)
      [ "$OPTARG" == "true" ] && repackage_corenlp=true || repackage_corenlp=false
      ;;
  esac
done

shift $((OPTIND - 1))

echo '----------------Downloading Downloader Dependencies-------------------'
if ( $download_libs ); then
	wget '-N' '--directory-prefix=../' 'http://central.maven.org/maven2/com/rometools/rome-utils/1.5.1/rome-utils-1.5.1.jar'
	wget '-N' '--directory-prefix=../' 'http://central.maven.org/maven2/com/rometools/rome/1.5.1/rome-1.5.1.jar'
	wget '-N' '--directory-prefix=../' 'http://central.maven.org/maven2/org/jdom/jdom2/2.0.6/jdom2-2.0.6.jar'
	wget '-N' '--directory-prefix=../' 'http://central.maven.org/maven2/org/slf4j/slf4j-api/1.7.12/slf4j-api-1.7.12.jar'
	wget '-N' '--directory-prefix=../' 'http://central.maven.org/maven2/org/slf4j/slf4j-simple/1.7.12/slf4j-simple-1.7.12.jar'
	wget '-N' '--directory-prefix=../' 'https://jdbc.postgresql.org/download/postgresql-9.4.1207.jar'
	wget '-N' '--directory-prefix=../' 'http://nlp.stanford.edu/software/stanford-corenlp-full-2015-12-09.zip'
	wget '-N' '--directory-prefix=../SEMILAR' 'http://deeptutor2.memphis.edu/Semilar-Web/public/downloads/SEMILAR-API-1.0.zip'
	wget '-N' '--directory-prefix=../SEMILAR' 'http://deeptutor2.memphis.edu/Semilar-Web/public/downloads/LSA-MODELS.zip'
	wget '-N' '--directory-prefix=../SEMILAR' 'http://deeptutor2.memphis.edu/Semilar-Web/public/downloads/LDA-MODELS.zip'
	unzip '-u' '../stanford-corenlp-full-2015-12-09' '-d' '../'
	wget '-N' '--directory-prefix=../' 'https://jdbc.postgresql.org/download/${key_library}'
else
	echo "Skipping."
fi

echo '-------------------Setting Up SEMILAR Libraries----------------------'
unzip '-u' '../SEMILAR/SEMILAR-API-1.0' '-d' '../SEMILAR/'
unzip '-u' '../SEMILAR/LSA-MODELS' '-d' '../SEMILAR/'
unzip '-u' '../SEMILAR/LDA-MODELS' '-d' '../SEMILAR/'

if ( $repackage_corenlp ); then
	unzip '-u' '../SEMILAR/SEMILAR-API-1.0/Semilar-1.0.jar' '-d' '../SEMILAR/SEMILAR-API-1.0/Semilar-1.0'
	perl -i -p0e $'s/Class-Path:.* \\.0\\.jar/Class-Path: lib\/joda-time.jar lib\/xom.jar lib\/opennlp-tools-1.5.0.jar \n lib\/edu.mit.jwi_2.1.5.jar lib\/jwnl-1.3.3.jar lib\/maxent-3.0.0.jar/smg' '../SEMILAR/SEMILAR-API-1.0/Semilar-1.0/META-INF/MANIFEST.MF'
	jar cfm '../SEMILAR/SEMILAR-API-1.0/Semilar-1.0.jar' '../SEMILAR/SEMILAR-API-1.0/Semilar-1.0/META-INF/MANIFEST.MF' -C '../SEMILAR/SEMILAR-API-1.0/Semilar-1.0/' '.'
	ant -buildfile 'repackage-corenlp.xml'
fi
rm '../SEMILAR/SEMILAR-API-1.0.zip'
rm '../SEMILAR/LSA-MODELS.zip'
rm '../SEMILAR/LDA-MODELS.zip'
rm -r '../SEMILAR/SEMILAR-API-1.0/Semilar-1.0'
rm '../stanford-corenlp-full-2015-12-09.zip'

echo '------------------Setting Up PostgreSQL Database---------------------'
if ( $setup_sql ); then
	initdb "$(brew --prefix)/var/postgres"
	mkdir -p "$HOME/Library/LaunchAgents"
	ln -sfv "$(brew --prefix)"/opt/postgresql/*.plist "$HOME/Library/LaunchAgents"
	[ "$(pg_ctl status | grep 'PID:' )" == "" ] && ( pg_ctl start > /dev/null )
	createdb event_detection
	psql event_detection < setup.sql
	psql event_detection < seeds.sql
else
	echo "Skipping."
fi

echo '------------------Generating Eclipse User Libraries------------------'
. "./generate_userlibs.sh"
