CREATE TYPE "received_from" AS ENUM (
  'cash',
  'bank_account',
  'paypal'
);

CREATE TABLE "person"
(
    "id"              integer PRIMARY KEY,
    "name"            TEXT NOT NULL,
    "room"            TEXT NOT NULL,
    "pays_floor_fees" bool NOT NULL,
    "account_id"      integer
);

CREATE TABLE "account"
(
    "id"        integer PRIMARY KEY,
    "name"      TEXT NOT NULL,
    "is_active" bool NOT NULL
);

CREATE TABLE "transaction"
(
    "id"                integer PRIMARY KEY,
    "source_account_id" integer   NOT NULL,
    "target_account_id" integer   NOT NULL,
    "amount"            integer   NOT NULL,
    "created_at"        timestamp NOT NULL,
    "description"       text      NOT NULL DEFAULT ''
);

CREATE TABLE "physical_transaction"
(
    "id"                      integer PRIMARY KEY,
    "transaction_id"          integer       NOT NULL,
    "source"                  received_from NOT NULL DEFAULT '',
    "created_at"              timestamp     NOT NULL,
    "transaction_description" text          NOT NULL DEFAULT ''
);

ALTER TABLE "person"
    ADD FOREIGN KEY ("account_id") REFERENCES "account" ("id");

ALTER TABLE "transaction"
    ADD FOREIGN KEY ("source_account_id") REFERENCES "account" ("id");

ALTER TABLE "transaction"
    ADD FOREIGN KEY ("target_account_id") REFERENCES "account" ("id");

ALTER TABLE "physical_transaction"
    ADD FOREIGN KEY ("transaction_id") REFERENCES "transaction" ("id");