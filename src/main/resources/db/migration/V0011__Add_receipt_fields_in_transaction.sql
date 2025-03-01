ALTER TABLE transaction ADD COLUMN receipt bytea DEFAULT NULL;
ALTER TABLE transaction ADD COLUMN receipt_mime_type TEXT DEFAULT NULL;