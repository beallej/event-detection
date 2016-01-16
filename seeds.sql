INSERT INTO sources (source_name, reliability) values ('CNN', 1.0);
INSERT INTO feeds (feed_name, source, url, scrapers) values ('CNN_US', (SELECT id FROM sources WHERE source_name = 'CNN'), 'http://rss.cnn.com/rss/cnn_us.rss', '{"CNN"}');
INSERT INTO SOURCES (source_name, reliability) VALUES ('NYTIMES', 1);
INSERT INTO feeds (feed_name, source, url, scrapers) values ('NYTIMES', (SELECT id FROM sources WHERE source_name = 'NYTIMES'), 'http://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml', '{"NYTIMES"}');
INSERT INTO users (email) values ('event.detection.carleton@gmail.com');
