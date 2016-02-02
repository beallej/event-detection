createdb event_detection_test
createdb event_detection_dev
psql event_detection_test < setup.sql
psql event_detection_dev < setup.sql
psql event_detection_test < setup_test.sql
psql event_detection_dev < setup_test.sql
