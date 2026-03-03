-- Add sync tracking columns to enable_banking_session
ALTER TABLE "enable_banking_session"
    ADD COLUMN "last_synced_at" TIMESTAMPTZ,
    ADD COLUMN "last_synced_date" DATE,
    ADD COLUMN "last_sync_status" TEXT,
    ADD COLUMN "last_sync_error_message" TEXT,
    ADD COLUMN "api_balance_cents" BIGINT,
    ADD COLUMN "api_balance_currency" TEXT,
    ADD COLUMN "computed_balance_cents" BIGINT,
    ADD COLUMN "balance_match" BOOLEAN,
    ADD COLUMN "opening_balance_cents" BIGINT;

-- Create bank_transaction table
CREATE TABLE "bank_transaction"
(
    "id"                      BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    "session_id"              BIGINT       NOT NULL REFERENCES "enable_banking_session" ("id") ON DELETE CASCADE,
    "entry_reference"         TEXT,
    "transaction_id"          TEXT,
    "amount_cents"            INTEGER      NOT NULL,
    "currency"                TEXT,
    "credit_debit_indicator"  TEXT,
    "booking_date"            DATE,
    "value_date"              DATE,
    "creditor_name"           TEXT,
    "debtor_name"             TEXT,
    "creditor_iban"           TEXT,
    "debtor_iban"             TEXT,
    "remittance_information"  TEXT,
    "status"                  TEXT,
    "created_at"              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Performance index
CREATE INDEX "idx_bank_transaction_session_booking"
    ON "bank_transaction" ("session_id", "booking_date");
