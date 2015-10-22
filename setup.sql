CREATE DATABASE IF NOT EXISTS eventdetection;
USE eventdetection;

CREATE TABLE IF NOT EXISTS sources (
	id varchar(255), reliability double,
	PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS feeds (
	id varchar(255), source varchar(255), url blob, scrapers blob, lastseen blob,
	primary key (id),
	foreign key (source) references sources(id)
);