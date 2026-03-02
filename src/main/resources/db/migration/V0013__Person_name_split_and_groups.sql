-- 1. Split name into first_name + last_name
ALTER TABLE person ADD COLUMN first_name TEXT;
ALTER TABLE person ADD COLUMN last_name TEXT;

-- Migrate: first word -> first_name, rest -> last_name
UPDATE person SET
  first_name = CASE
    WHEN position(' ' in name) > 0 THEN left(name, position(' ' in name) - 1)
    ELSE name
  END,
  last_name = CASE
    WHEN position(' ' in name) > 0 THEN substring(name from position(' ' in name) + 1)
    ELSE ''
  END;

ALTER TABLE person ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE person ALTER COLUMN last_name SET NOT NULL;
ALTER TABLE person DROP COLUMN name;

-- 2. Add Keycloak user ID (UUID subject from KC)
ALTER TABLE person ADD COLUMN keycloak_user_id TEXT;

-- 3. Group table
CREATE TABLE "group" (
    "id"                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "name"                TEXT NOT NULL UNIQUE,
    "keycloak_group_id"   TEXT
);

-- 4. Person-Group association (N:M)
CREATE TABLE "person_group" (
    "person_id" BIGINT NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    "group_id"  BIGINT NOT NULL REFERENCES "group"(id) ON DELETE CASCADE,
    PRIMARY KEY (person_id, group_id)
);
