ALTER TABLE IF EXISTS query_articles ADD COLUMN validates boolean default false;
INSERT INTO sources (source_name, reliability) VALUES ('TEST_SOURCE', 1);