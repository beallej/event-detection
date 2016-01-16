INSERT INTO SOURCES (source_name, reliability) VALUES ('NYTIMES', 1);
INSERT INTO feeds (feed_name, source, url, scrapers) values ('NYTIMES', (SELECT id FROM sources WHERE source_name = 'NYTIMES'), 'http://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml', '{"NYTIMES"}');
