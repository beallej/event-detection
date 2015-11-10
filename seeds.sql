INSERT INTO sources (source_name, reliability) values ('CNN', 1.0);
INSERT INTO feeds (feed_name, source, url, scrapers) values ('CNN_US', 1, 'http://rss.cnn.com/rss/cnn_us.rss', '{"CNN"}');
INSERT INTO users (user_name, email) values ('dummy', 'event.detection.carleton@gmail.com');
INSERT INTO notification_types (type_name, description) values ('text', 'Send a text message to a phone number'), ('email', 'send an email to an email address');
