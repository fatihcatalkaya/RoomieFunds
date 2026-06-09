CREATE TABLE hbci_config (
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    blz                 TEXT NOT NULL,
    username            TEXT NOT NULL,
    encrypted_pin       TEXT NOT NULL,
    encrypted_passport  TEXT,
    account_id          BIGINT NOT NULL,
    last_synced_at      TIMESTAMPTZ,
    UNIQUE (account_id)
);
ALTER TABLE hbci_config ADD FOREIGN KEY (account_id) REFERENCES account (id);

CREATE TABLE account_iban (
    id          BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id  BIGINT NOT NULL,
    iban        TEXT NOT NULL,
    UNIQUE (iban)
);
ALTER TABLE account_iban ADD FOREIGN KEY (account_id) REFERENCES account (id);
CREATE INDEX ON account_iban (account_id);

CREATE TABLE hbci_sync_log (
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id      BIGINT NOT NULL,
    synced_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    imported_count  INT NOT NULL DEFAULT 0,
    skipped_count   INT NOT NULL DEFAULT 0,
    success         BOOLEAN NOT NULL,
    error_message   TEXT
);
ALTER TABLE hbci_sync_log ADD FOREIGN KEY (account_id) REFERENCES account (id);
CREATE INDEX ON hbci_sync_log (account_id);
