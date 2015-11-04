CREATE USER root with SUPERUSER CREATEDB CREATEROLE INHERIT;

CREATE TABLE IF NOT EXISTS sources (
	id varchar(255), reliability real,
	PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS feeds (
	id varchar(255), source varchar(255), url text, scrapers text, lastseen text default NULL,
	primary key (id),
	foreign key (source) references sources(id)
);

CREATE TABLE IF NOT EXISTS articles (
	id serial, title varchar(255), source varchar(255), url text, filename text default NULL, text[] keywords,
	primary key (id),
	foreign key (source) references sources(id)
);
