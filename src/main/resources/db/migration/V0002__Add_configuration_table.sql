CREATE TABLE "settings"
(
    "setting_key"  text UNIQUE PRIMARY KEY NOT NULL,
    "value_int"    BIGINT,
    "value_string" TEXT
);