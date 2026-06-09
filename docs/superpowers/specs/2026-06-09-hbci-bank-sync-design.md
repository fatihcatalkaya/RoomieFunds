# HBCI Bank Sync — Design Spec

**Date:** 2026-06-09
**Branch:** feat-bank-sync

---

## Overview

Automatically import incoming payments from shared bank accounts into RoomieFunds by fetching transactions via HBCI/FinTS using the hbci4java library. Multiple bank account configurations are supported — one HBCI config per internal account (enforced by a UNIQUE constraint). Only incoming credits whose counterparty IBAN matches a known person account are imported. All other transactions are skipped.

---

## Constraints & Decisions

| Topic | Decision |
|---|---|
| Library | hbci4java (`KUmsAllCamt` job) |
| Authentication | PIN/TAN — pushTAN Decoupled (admin taps Accept in banking app; no TAN code to type) |
| Trigger | Admin-triggered only via REST endpoint; no background scheduler |
| HTTP model | Long-poll: frontend sends `POST /configs/{accountId}/sync`, shows spinner, awaits response (3-min timeout) |
| Import filter | Incoming credits only (`value > 0`); counterparty IBAN must match a known account |
| Unmatched transactions | Skipped silently; counted in sync log |
| Outgoing debits | Ignored entirely |
| Deduplication | CAMT transaction ID stored on `transaction.camt_id` with UNIQUE constraint |
| Passport storage | AES-encrypted blob in `hbci_config.encrypted_passport`; written to temp file at runtime, saved back after each use |
| Credential encryption | AES-256-GCM; key from `HBCI_AES_KEY` environment variable; same key for PIN and passport blob |
| Config identifier | `account_id` is the public identifier for all config endpoints (UNIQUE on `hbci_config.account_id`); the internal surrogate PK is not exposed in the API |

---

## Data Model

### V0016 — New tables

```sql
CREATE TABLE hbci_config (
    id                  BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    blz                 TEXT NOT NULL,
    username            TEXT NOT NULL,
    encrypted_pin       TEXT NOT NULL,     -- AES-256-GCM encrypted, base64-encoded
    encrypted_passport  TEXT,              -- AES-256-GCM encrypted hbci4java passport blob; NULL until first connect
    account_id          BIGINT NOT NULL,
    last_synced_at      TIMESTAMPTZ,       -- start of next fetch window; NULL = never synced
    UNIQUE (account_id)                    -- one HBCI config per bank account
);
ALTER TABLE hbci_config ADD FOREIGN KEY (account_id) REFERENCES account (id);

CREATE TABLE account_iban (
    id          BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id  BIGINT NOT NULL,
    iban        TEXT NOT NULL,
    UNIQUE (iban)                          -- one IBAN maps to exactly one account
);
ALTER TABLE account_iban ADD FOREIGN KEY (account_id) REFERENCES account (id);

CREATE TABLE hbci_sync_log (
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id      BIGINT NOT NULL,       -- the bank account that was synced
    synced_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    imported_count  INT NOT NULL,
    skipped_count   INT NOT NULL,          -- unmatched incoming credits
    success         BOOLEAN NOT NULL,
    error_message   TEXT                   -- NULL on success
);
ALTER TABLE hbci_sync_log ADD FOREIGN KEY (account_id) REFERENCES account (id);
```

### V0017 — Extend `transaction`

```sql
ALTER TABLE transaction ADD COLUMN camt_id TEXT UNIQUE;
-- NULL for manually-entered transactions (Postgres UNIQUE allows multiple NULLs)
-- Set to buchung.id for HBCI-imported transactions
```

### Transaction direction

An incoming payment from a roommate is recorded as:
- `source_account_id` = person's internal account (payer, resolved via `account_iban`)
- `target_account_id` = `hbci_config.account_id` (the shared bank account)
- `description` = `buchung.usage` lines joined
- `value_date` = `buchung.valuta`
- `camt_id` = `buchung.id`

### Fetch date range

- **First sync:** `[today - ${app.hbci.initial-lookback-days:-90}, today]`
- **Subsequent syncs:** `[last_synced_at - 3 days, today]` (3-day overlap catches late-posted entries)

---

## Domain Layer

Package root: `domain/api/hbcisync/` and `domain/spi/`

### API interfaces

| Interface | Responsibility |
|---|---|
| `GetHbciConfig` | List all configs and get one by `accountId` (never exposes decrypted PIN); list IBAN mappings |
| `SaveHbciConfig` | Create, update, or delete a config by `accountId` (accepts plaintext PIN, service encrypts before persisting); manage IBAN mappings |
| `SyncBankTransactions` | Trigger HBCI fetch + import for the config identified by `accountId`; returns `HbciSyncResult` |

### SPI interfaces

| Interface | Responsibility |
|---|---|
| `HbciConfigRepository` | CRUD for `hbci_config` keyed by `accountId`; load/save passport blob by `accountId`; update `last_synced_at` by `accountId` |
| `AccountIbanRepository` | CRUD for `account_iban`; `findAccountByIban(String): Optional<Long>` |
| `HbciSyncLogRepository` | Append-only write of `hbci_sync_log` rows; keyed by `accountId` |
| `HbciClient` | Port for hbci4java interaction; `fetchTransactions(HbciCredentials, DateRange): HbciFetchResult` |

`HbciFetchResult` carries a list of `HbciTransactionEntry` (a domain record) plus updated passport bytes. `HbciClientImpl` maps each `UmsLine` to `HbciTransactionEntry` before returning, keeping hbci4java types out of the domain entirely.

**`HbciTransactionEntry`** (domain record):
- `camtId: String` — `buchung.id`
- `valueDate: LocalDate` — `buchung.valuta`
- `amountCents: int` — `buchung.value` converted to cents (positive = credit, negative = debit)
- `counterpartyIban: String` — `buchung.other.iban` (nullable)
- `usage: String` — `buchung.usage` lines joined

### `HbciSyncService` — `sync(long accountId)` flow

1. Load config from `HbciConfigRepository.loadCredentials(accountId)`; decrypt PIN and passport blob via `AesEncryptionService`
2. Call `HbciClient.fetchTransactions(...)` — blocks until phone approval
3. Save updated passport bytes back via `HbciConfigRepository.savePassportBytes(accountId, ...)` **in its own transaction** (independent of step 4; must not roll back if import fails)
4. For each `HbciTransactionEntry`:
   - Skip if `amountCents ≤ 0` (outgoing/debit)
   - Skip if `camtId` already in `transaction` table (dedup)
   - Look up counterparty IBAN via `AccountIbanRepository`; skip if no match
   - Insert `transaction` row via `TransactionRepository`
5. Update `last_synced_at` for this account; write `hbci_sync_log` row (keyed by `accountId`)
6. Return `HbciSyncResult`

`HbciSyncContext` (bootstrap) wires the service — same pattern as `RecurringTransactionContext`.

---

## Infrastructure Layer

### `HbciClientImpl`

- Implements `HbciClient`, `@ApplicationScoped`
- Writes decrypted passport bytes to `File.createTempFile()`
- Initialises `HBCIUtils` with a callback answering: `NEED_BLZ`, `NEED_USERID`, `NEED_CUSTOMERID`, `NEED_PT_PIN`, `NEED_PASSPHRASE_LOAD/SAVE`, `NEED_PT_DECOUPLED` (log + return; hbci4java polls bank internally)
- Executes `KUmsAllCamt` job for the given date range
- Reads updated passport file bytes, deletes temp file, returns `HbciFetchResult`
- Not tested against a real bank via automated tests; validated manually with the existing `HBCI-Test` project

### `HbciConfigRepositoryImpl`

- jOOQ, `@ApplicationScoped`
- Injects `AesEncryptionService` (reads `HBCI_AES_KEY` env var, AES-256-GCM)
- Encrypts PIN and passport blob before writes; decrypts after reads
- All lookups use `HBCI_CONFIG.ACCOUNT_ID` — the surrogate PK is never used externally

### `AesEncryptionService`

- `@ApplicationScoped` utility; no domain dependency
- `encrypt(String): String` / `encryptBytes(byte[]): String` (base64-encoded ciphertext+IV)
- `decrypt(String): String` / `decryptBytes(String): byte[]`
- Key sourced from `HBCI_AES_KEY` environment variable

### `AccountIbanRepositoryImpl` / `HbciSyncLogRepositoryImpl`

- jOOQ, straightforward

### REST — `HbciController` (`@Path("/api/hbci")`)

| Method | Path | Description |
|---|---|---|
| `GET` | `/configs` | List all configs (PINs masked) |
| `POST` | `/configs` | Create a new config |
| `GET` | `/configs/{accountId}` | Get config for a specific account |
| `PUT` | `/configs/{accountId}` | Update credentials for a specific account's config |
| `DELETE` | `/configs/{accountId}` | Delete a specific account's config |
| `POST` | `/configs/{accountId}/sync` | Trigger sync for that account (3-min HTTP timeout) |
| `GET` | `/ibans` | List all IBAN → account mappings |
| `POST` | `/ibans` | Add an IBAN mapping |
| `DELETE` | `/ibans/{id}` | Remove an IBAN mapping |

---

## Error Handling

| Scenario | Behaviour |
|---|---|
| Bank unreachable / wrong credentials | `HbciClientImpl` catches `HBCI_Exception`, wraps in `HbciSyncException`; service writes failed log row; REST returns 502 |
| Config not found for accountId | `HbciSyncException` thrown; REST controller maps to 404 |
| Partial import DB error | Entire import runs `@Transactional`; any failure rolls back completely; sync log written in separate transaction |
| Duplicate CAMT ID (DB constraint violation) | Caught; counted as skip, not error |
| Passport write-back failure | Log warning; old passport bytes remain; hbci4java re-fetches BPD/UPD on next connect |
| Phone approval timeout | hbci4java throws `HBCI_Exception`; handled as connection failure |

---

## Out of Scope

- Outgoing debit import
- Manual mapping UI for unrecognised transactions
- Automated background scheduling
