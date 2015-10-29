CREATE DATABASE IF NOT EXISTS event_detection;
USE event_detection;

CREATE TABLE IF NOT EXISTS sources (
	id varchar(255), reliability double,
	PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS feeds (
	id varchar(255), source varchar(255), url blob, scrapers blob, lastseen blob default NULL,
	primary key (id),
	foreign key (source) references sources(id)
);

CREATE TABLE IF NOT EXISTS articles (
	id INT UNSIGNED AUTO_INCREMENT, title varchar(255), source varchar(255), url blob, filename blob default NULL,
	primary key (id),
	foreign key (source) references sources(id),
	unique key (filename(100))
);
