BEGIN;
ALTER TABLE regimens ADD COLUMN versionCode INTEGER;
COMMIT;
