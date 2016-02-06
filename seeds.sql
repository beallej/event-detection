INSERT INTO sources (source_name, reliability) values ('CNN', 1.0);
INSERT INTO feeds (feed_name, source, url, scrapers) values ('CNN_US', (SELECT id FROM sources WHERE source_name = 'CNN'), 'http://rss.cnn.com/rss/cnn_us.rss', '{"CNN"}');
INSERT INTO users (email) values ('event.detection.carleton@gmail.com');
INSERT INTO validation_algorithms (algorithm) values ('keyword');
INSERT INTO validation_algorithms (algorithm) values ('Swoogle Semantic Analysis');
INSERT INTO validation_algorithms (algorithm) values ('SEMILAR Semantic Analysis');
