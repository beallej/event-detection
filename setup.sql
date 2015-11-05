CREATE USER root with SUPERUSER CREATEDB CREATEROLE INHERIT;

CREATE TABLE IF NOT EXISTS sources (
	id varchar(255),
	reliability real,
	primary key (id)
);

CREATE TABLE IF NOT EXISTS feeds (
	id varchar(255),
	source varchar(255) references sources(id) ON DELETE CASCADE,
	url text,
	scrapers text,
	lastseen text default NULL,
	primary key (id)
);

CREATE TABLE IF NOT EXISTS articles (
	id serial,
	title varchar(255),
	source varchar(255) references sources(id) ON DELETE CASCADE,
	url text,
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
	word varchar(255),
	pos varchar(255),
	sense varchar(255) default NULL,
	synonyms varchar(255)[],
	primary key (word, pos, sense),
	constraint word_pos unique(word, pos)
);

CREATE TABLE IF NOT EXISTS query_articles (
	query integer references queries(id) ON DELETE CASCADE,
	article integer references articles(id) ON DELETE CASCADE,
	primary key (query, article)
);
