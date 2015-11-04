CREATE USER root with SUPERUSER CREATEDB CREATEROLE INHERIT;

CREATE TABLE IF NOT EXISTS sources (
	id varchar(255), reliability real,
	PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS feeds (
	id varchar(255), source varchar(255) references sources(id) ON DELETE CASCADE, url text, scrapers text, lastseen text default NULL,
	primary key (id)
);

CREATE TABLE IF NOT EXISTS articles (
	id serial, title varchar(255), source varchar(255) references sources(id) ON DELETE CASCADE, url text, filename text default NULL, keywords text[] default NULL,
	primary key (id)
);

CREATE TABLE IF NOT EXISTS queries (
	id serial, query text,
	primary key (id)
);

CREATE TABLE IF NOT EXISTS query_article_map (
	query integer references queries(id) ON DELETE CASCADE, article integer references articles(id) ON DELETE CASCADE,
	primary key (query, article)
);
