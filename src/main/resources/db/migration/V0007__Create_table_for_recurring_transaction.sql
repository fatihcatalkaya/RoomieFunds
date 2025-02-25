CREATE TABLE "recurring_transaction"
(
    "id"                      bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "amount"                  int    NOT NULL,
    "source_account_id"       bigint NOT NULL,
    "target_account_id"       bigint NOT NULL,
    "value_day_of_month"      int    NOT NULL,
    "name"                    text   NOT NULL DEFAULT '',
    "transaction_description" text   NOT NULL DEFAULT ''
);
COMMENT ON COLUMN "recurring_transaction"."amount" IS 'in cent';
ALTER TABLE "recurring_transaction"
    ADD FOREIGN KEY ("source_account_id") REFERENCES "account" ("id");
ALTER TABLE "recurring_transaction"
    ADD FOREIGN KEY ("target_account_id") REFERENCES "account" ("id");