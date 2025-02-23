CREATE TABLE "flurbeitrag_scheduler_log"
(
    "id"         bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "created_at" DATE UNIQUE NOT NULL
);

CREATE TABLE "recurring_transaction_scheduler_log"
(
    "id"                         bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "created_at"                 DATE   NOT NULL,
    "recurring_transaction_id"   BIGINT NOT NULL,
    "recurring_transaction_name" TEXT   NOT NULL
);

COMMENT ON COLUMN "recurring_transaction_scheduler_log"."recurring_transaction_id" IS 'no reference to recurring transaction table, because it might get deleted';