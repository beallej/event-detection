INSERT INTO sources (source_name, reliability) values ('CNN', 1.0), ('REUTERS', 1.0), ('ABC', 1.0);
INSERT INTO feeds (feed_name, source, url, scrapers) values ('CNN_US', (SELECT id FROM sources WHERE source_name = 'CNN'), 'http://rss.cnn.com/rss/cnn_us.rss', '{"CNN"}');
INSERT INTO feeds (feed_name, source, url, scrapers) values ('REUTERS_TOP_NEWS', (SELECT id FROM sources WHERE source_name = 'REUTERS'), 'http://feeds.reuters.com/reuters/topNews', '{"REUTERS"}');
INSERT INTO feeds (feed_name, source, url, scrapers) values ('ABC_TOP_STORIES', (SELECT id FROM sources WHERE source_name = 'ABC'), 'http://feeds.abcnews.com/abcnews/topstories', '{"ABC"}');
INSERT INTO users (email) values ('event.detection.carleton@gmail.com');
INSERT INTO validation_algorithms (algorithm, base_class, validator_type, parameters) values
	('Keyword', 'eventdetection.validator.implementations.KeywordValidator', 'OneToOne', '"KeywordValidator.json"');
INSERT INTO validation_algorithms (algorithm, base_class, validator_type, parameters) values
	('Swoogle Semantic Analysis', 'eventdetection.validator.implementations.SwoogleSemanticAnalysisValidator', 'OneToOne',
		'{"instance" : {"url-prefix" : "http://swoogle.umbc.edu/StsService/GetStsSim?operation=api", "max-sentences" : 5}}');
INSERT INTO validation_algorithms (algorithm, base_class, validator_type, parameters) values
	('SEMILAR Semantic Analysis', 'eventdetection.validator.implementations.SEMILARSemanticAnalysisValidator', 'OneToOne', '"SEMILARSemanticAnalysisValidator.json"');
INSERT INTO validation_algorithms (algorithm, base_class, validator_type, parameters) values
	('TextRank Swoogle Semantic Analysis', 'eventdetection.validator.implementations.TextRankSwoogleSemanticAnalysisValidator', 'OneToOne',
		'{"instance" : {"url-prefix" : "http://swoogle.umbc.edu/StsService/GetStsSim?operation=api"}}');
INSERT INTO validation_algorithms (algorithm, base_class, validator_type, parameters) values
	('Clustering', 'eventdetection.validator.implementations.ClusteringValidator', 'ManyToMany', null);
