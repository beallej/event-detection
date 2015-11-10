CREATE USER root with SUPERUSER CREATEDB CREATEROLE INHERIT;

CREATE TABLE IF NOT EXISTS sources (
	id serial primary key unique not null,
	source_name varchar(255) unique not null,
	reliability real,
	check (reliability <= 1.0)
);

CREATE TABLE IF NOT EXISTS feeds (
	id serial primary key unique not null,
	feed_name varchar(255) unique not null,
	source integer not null references sources(id) ON DELETE CASCADE,
	url text unique not null,
	scrapers text[] default '{}',
	lastseen text default null
);

CREATE TABLE IF NOT EXISTS articles (
	id serial primary key unique not null,
	title varchar(255) not null,
	source integer not null references sources(id) ON DELETE CASCADE,
	url text unique not null,
	filename text default null,
	keywords text[] default null
);

CREATE TABLE IF NOT EXISTS users (
	id serial primary key unique not null,
	user_name text unique not null
);

CREATE TABLE IF NOT EXISTS notification_types (
	id serial primary key unique not null,
	type_name text unique not null,
	description text default ''
);

CREATE TABLE IF NOT EXISTS user_contact_info (
	user_id integer not null references users(id) ON DELETE CASCADE,
	notification_type integer not null references notification_types(id) ON DELETE CASCADE,
	arguments text[] default '{}',
	display_name text not null,
	primary key (user_id, notification_type, arguments)
);

CREATE TABLE IF NOT EXISTS queries (
	id serial primary key unique not null,
	userid integer not null references users(id) ON DELETE CASCADE,
	subject varchar(255),
	verb varchar(255),
	direct_obj varchar(255),
	indirect_obj varchar(255),
	loc varchar(255),
	constraint unique_queries unique (userid, subject, verb, direct_obj, indirect_obj, loc)
);

CREATE TABLE IF NOT EXISTS query_notifications (
	query_id integer not null references queries(id) ON DELETE CASCADE,
	user_id integer not null references users(id),
	notification_type integer not null references notification_types(id) ON DELETE CASCADE,
	arguments text[] default '{}',
	foreign key (user_id, notification_type, arguments) references user_contact_info (user_id, notification_type, arguments),
	constraint unique_notification unique (query_id, notification_type, arguments)
);

CREATE TABLE IF NOT EXISTS query_words (
	word varchar(255) not null,
	pos varchar(255) not null,
	sense varchar(255) default null,
	synonyms varchar(255)[] default '{}',
	primary key (word, pos, sense),
	constraint word_pos unique(word, pos)
);

CREATE TABLE IF NOT EXISTS query_articles (
	query integer references queries(id) ON DELETE CASCADE,
	article integer references articles(id) ON DELETE CASCADE,
	accuracy real default 1.0,
	primary key (query, article)
);
