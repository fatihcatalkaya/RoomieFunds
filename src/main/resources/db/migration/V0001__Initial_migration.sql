CREATE TYPE "log_operations" AS ENUM (
  'create',
  'update',
  'delete'
);

CREATE TYPE "received_from" AS ENUM (
  'cash',
  'bank_account',
  'paypal'
);

CREATE TABLE "log"
(
    "id"                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "created_at"          timestamptz    NOT NULL DEFAULT now(),
    "operation"           log_operations NOT NULL,
    "modified_table_name" text           NOT NULL,
    "modified_by"         text           NOT NULL,
    "subject_before"      jsonb,
    "subject_after"       jsonb
);

CREATE TABLE "product"
(
    "id"    BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "name"  TEXT    NOT NULL,
    "price" integer NOT NULL
);

CREATE TABLE "person"
(
    "id"              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "name"            TEXT NOT NULL,
    "room"            TEXT NOT NULL,
    "pays_floor_fees" bool NOT NULL,
    "account_id"      BIGINT
);

CREATE TABLE "account"
(
    "id"        BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "name"      TEXT NOT NULL,
    "is_active" bool NOT NULL
);

CREATE TABLE "transaction"
(
    "id"                BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "source_account_id" BIGINT     NOT NULL,
    "target_account_id" BIGINT     NOT NULL,
    "amount"            integer     NOT NULL,
    "created_at"        timestamptz NOT NULL,
    "description"       text        NOT NULL DEFAULT ''
);

CREATE TABLE "physical_transaction"
(
    "id"                      BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "transaction_id"          BIGINT       NOT NULL,
    "source"                  received_from NOT NULL,
    "created_at"              timestamp     NOT NULL,
    "transaction_description" text          NOT NULL DEFAULT ''
);

COMMENT
ON COLUMN "log"."modified_by" IS 'username parsed from jwt token';

COMMENT
ON COLUMN "product"."price" IS 'in cent';

COMMENT
ON COLUMN "transaction"."amount" IS 'in cent';

ALTER TABLE "person"
    ADD FOREIGN KEY ("account_id") REFERENCES "account" ("id");

ALTER TABLE "transaction"
    ADD FOREIGN KEY ("source_account_id") REFERENCES "account" ("id");

ALTER TABLE "transaction"
    ADD FOREIGN KEY ("target_account_id") REFERENCES "account" ("id");

ALTER TABLE "physical_transaction"
    ADD FOREIGN KEY ("transaction_id") REFERENCES "transaction" ("id");