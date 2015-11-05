CREATE USER root with SUPERUSER CREATEDB CREATEROLE INHERIT;

CREATE TABLE IF NOT EXISTS sources (
	id serial unique not null,
	source_name varchar(255) unique not null,
	reliability real,
	primary key (id),
	check (reliability <= 1.0)
);

CREATE TABLE IF NOT EXISTS feeds (
	id serial unique not null,
	feed_name varchar(255) unique not null,
	source integer not null references sources(id) ON DELETE CASCADE,
	url text unique not null,
	scrapers text[] default '{}',
	lastseen text default NULL,
	primary key (id)
);

CREATE TABLE IF NOT EXISTS articles (
	id serial unique not null,
	title varchar(255) unique not null,
	source integer not null references sources(id) ON DELETE CASCADE,
	url text unique not null,
	filename text default NULL,
	keywords text[] default NULL,
	primary key (id)
);

CREATE TABLE IF NOT EXISTS queries (
	id serial,
	subject varchar(255),
	verb varchar(255),
	direct_obj varchar(255),
	indirect_obj varchar(255),
	loc varchar(255),
	primary key (id)
);

CREATE TABLE IF NOT EXISTS query_words (
	word varchar(255) not null,
	pos varchar(255) not null,
	sense varchar(255) default NULL,
	synonyms varchar(255)[] default '{}',
	primary key (word, pos, sense),
	constraint word_pos unique(word, pos)
);

CREATE TABLE IF NOT EXISTS query_articles (
	query integer references queries(id) ON DELETE CASCADE,
	article integer references articles(id) ON DELETE CASCADE,
	primary key (query, article)
);
