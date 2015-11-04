CREATE USER root with SUPERUSER CREATEDB CREATEROLE INHERIT;

/* One entry per query */
CREATE TABLE IF NOT EXISTS queries {
	query_id serial,
	subject varchar(255),
	verb varchar(255),
	direct_obj varchar(255),
	indirect_obj varchar(255),
	location varchar(255), 
	PRIMARY KEY (id)
};

/* One entry for each synonym of each word in each query */
CREATE TABLE IF NOT EXISTS query_words {
	query_id integer,
	query_word varchar(255),
	pos varchar(255),
	synonym varchar(255)
};

/* One entry per keyword in each article;
   'articles' table already stores 
   title, url, source, ... */
CREATE TABLE IF NOT EXISTS article_keywords {
	article_id integer,
	keyword varchar(255)
};

/* One entry per matched query-article pair */
CREATE TABLE IF NOT EXISTS query_articles {
	query_id integer,
	article_id integer
};