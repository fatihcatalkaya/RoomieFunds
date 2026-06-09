# HBCI Bank Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Import incoming bank payments automatically via HBCI/FinTS into RoomieFunds when an admin triggers a sync and approves the pushTAN on their phone. Multiple HBCI configurations are supported; the public identifier for all config endpoints is the `account_id` (UNIQUE on `hbci_config`).

**Architecture:** Admin-triggered long-poll REST endpoint calls `HbciSyncService.sync(accountId)`, which fetches transactions via `HbciClientImpl` (hbci4java), matches counterparty IBANs to internal accounts, and inserts transactions with CAMT IDs for deduplication. Credentials and passport state are stored AES-256-GCM encrypted in the database per config row.

**Tech Stack:** Quarkus 3.20.2, Java 21, jOOQ 3.20.4, Flyway, PostgreSQL, hbci4java 4.1.11 (already in pom.xml)

**Spec:** `docs/superpowers/specs/2026-06-09-hbci-bank-sync-design.md`

---

## File Map

**New files:**
```
src/main/resources/db/migration/
  V0016__HBCI_tables.sql
  V0017__Add_camt_id_to_transaction.sql

src/main/java/de/flur4/roomiefunds/
  models/hbci/
    HbciConfig.java
    SaveHbciConfigDto.java
    AccountIban.java
    CreateAccountIbanDto.java
    HbciSyncResult.java
    HbciTransactionEntry.java
    HbciCredentials.java
    HbciDateRange.java
    HbciFetchResult.java
    HbciSyncException.java
  domain/spi/
    HbciConfigRepository.java
    AccountIbanRepository.java
    HbciSyncLogRepository.java
    HbciClient.java
  domain/api/hbcisync/
    GetHbciConfig.java
    SaveHbciConfig.java
    SyncBankTransactions.java
    impl/HbciSyncService.java
  bootstrap/
    HbciSyncContext.java
  infrastructure/
    hbci/
      AesEncryptionService.java
      HbciClientImpl.java
    repository/
      HbciConfigRepositoryImpl.java
      AccountIbanRepositoryImpl.java
      HbciSyncLogRepositoryImpl.java
    web/
      HbciController.java
```

**Modified files:**
```
src/main/java/de/flur4/roomiefunds/domain/spi/TransactionRepository.java
src/main/java/de/flur4/roomiefunds/infrastructure/repository/TransactionRepositoryImpl.java
src/main/resources/application.properties
```

---

## Task 1: DB Migrations

**Files:**
- Create: `src/main/resources/db/migration/V0016__HBCI_tables.sql`
- Create: `src/main/resources/db/migration/V0017__Add_camt_id_to_transaction.sql`

- [ ] **Step 1: Create V0016**

```sql
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

CREATE TABLE hbci_sync_log (
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    account_id      BIGINT NOT NULL,
    synced_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    imported_count  INT NOT NULL,
    skipped_count   INT NOT NULL,
    success         BOOLEAN NOT NULL,
    error_message   TEXT
);
ALTER TABLE hbci_sync_log ADD FOREIGN KEY (account_id) REFERENCES account (id);
```

- [ ] **Step 2: Create V0017**

```sql
ALTER TABLE transaction ADD COLUMN camt_id TEXT UNIQUE;
```

- [ ] **Step 3: Regenerate jOOQ sources**

Run: `mvn generate-sources -Djooq-local`

Expected: BUILD SUCCESS. New jOOQ classes appear for `HbciConfig`, `AccountIban`, `HbciSyncLog`. `Transaction` gains a `CAMT_ID` field.

- [ ] **Step 4: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V0016__HBCI_tables.sql \
        src/main/resources/db/migration/V0017__Add_camt_id_to_transaction.sql
git commit -m "feat: add HBCI and account_iban DB migrations"
```

---

## Task 2: Domain Models

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/models/hbci/HbciConfig.java`
- Create: `src/main/java/de/flur4/roomiefunds/models/hbci/SaveHbciConfigDto.java`
- Create: `src/main/java/de/flur4/roomiefunds/models/hbci/AccountIban.java`
- Create: `src/main/java/de/flur4/roomiefunds/models/hbci/CreateAccountIbanDto.java`
- Create: `src/main/java/de/flur4/roomiefunds/models/hbci/HbciSyncResult.java`
- Create: `src/main/java/de/flur4/roomiefunds/models/hbci/HbciTransactionEntry.java`
- Create: `src/main/java/de/flur4/roomiefunds/models/hbci/HbciCredentials.java`
- Create: `src/main/java/de/flur4/roomiefunds/models/hbci/DateRange.java`
- Create: `src/main/java/de/flur4/roomiefunds/models/hbci/HbciFetchResult.java`
- Create: `src/main/java/de/flur4/roomiefunds/models/hbci/HbciSyncException.java`

- [ ] **Step 1: Create HbciConfig**

```java
package de.flur4.roomiefunds.models.hbci;

import java.time.OffsetDateTime;

public record HbciConfig(long id, String blz, String username, long accountId, OffsetDateTime lastSyncedAt) {
}
```

- [ ] **Step 2: Create SaveHbciConfigDto**

```java
package de.flur4.roomiefunds.models.hbci;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SaveHbciConfigDto(@NotBlank String blz,
                                @NotBlank String username,
                                @NotBlank String pin,
                                @Positive long accountId) {
}
```

- [ ] **Step 3: Create AccountIban**

```java
package de.flur4.roomiefunds.models.hbci;

public record AccountIban(long id, long accountId, String iban) {
}
```

- [ ] **Step 4: Create CreateAccountIbanDto**

```java
package de.flur4.roomiefunds.models.hbci;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateAccountIbanDto(@Positive long accountId, @NotBlank String iban) {
}
```

- [ ] **Step 5: Create HbciSyncResult**

```java
package de.flur4.roomiefunds.models.hbci;

public record HbciSyncResult(int importedCount, int skippedCount) {
}
```

- [ ] **Step 6: Create HbciTransactionEntry**

```java
package de.flur4.roomiefunds.models.hbci;

import java.time.LocalDate;

public record HbciTransactionEntry(String camtId,
                                   LocalDate valueDate,
                                   int amountCents,
                                   String counterpartyIban,
                                   String usage) {
}
```

- [ ] **Step 7: Create HbciCredentials**

```java
package de.flur4.roomiefunds.models.hbci;

import java.time.OffsetDateTime;

public record HbciCredentials(String blz,
                              String username,
                              String decryptedPin,
                              byte[] passportBytes,
                              long accountId,
                              OffsetDateTime lastSyncedAt) {
}
```

- [ ] **Step 8: Create DateRange**

```java
package de.flur4.roomiefunds.models.hbci;

import java.time.LocalDate;

public record DateRange(LocalDate from, LocalDate to) {
}
```

- [ ] **Step 9: Create HbciFetchResult**

```java
package de.flur4.roomiefunds.models.hbci;

import java.util.List;

public record HbciFetchResult(List<HbciTransactionEntry> entries, byte[] updatedPassportBytes) {
}
```

- [ ] **Step 10: Create HbciSyncException**

```java
package de.flur4.roomiefunds.models.hbci;

public class HbciSyncException extends RuntimeException {
    public HbciSyncException(String message) {
        super(message);
    }

    public HbciSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 11: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 12: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/models/hbci/
git commit -m "feat: add HBCI domain model records"
```

---

## Task 3: Domain SPI Interfaces

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/domain/spi/HbciConfigRepository.java`
- Create: `src/main/java/de/flur4/roomiefunds/domain/spi/AccountIbanRepository.java`
- Create: `src/main/java/de/flur4/roomiefunds/domain/spi/HbciSyncLogRepository.java`
- Create: `src/main/java/de/flur4/roomiefunds/domain/spi/HbciClient.java`

- [ ] **Step 1: Create HbciConfigRepository**

All methods are keyed by `accountId` — the surrogate PK is never used outside the repository.

```java
package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.hbci.HbciConfig;
import de.flur4.roomiefunds.models.hbci.HbciCredentials;
import de.flur4.roomiefunds.models.hbci.SaveHbciConfigDto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface HbciConfigRepository {
    List<HbciConfig> findAll();
    Optional<HbciConfig> findByAccountId(long accountId);
    Optional<HbciCredentials> loadCredentials(long accountId);
    HbciConfig createConfig(SaveHbciConfigDto dto);
    void updateConfig(long accountId, SaveHbciConfigDto dto);
    void deleteByAccountId(long accountId);
    void savePassportBytes(long accountId, byte[] passportBytes);
    void updateLastSyncedAt(long accountId, OffsetDateTime syncedAt);
}
```

- [ ] **Step 2: Create AccountIbanRepository**

```java
package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;

import java.util.List;
import java.util.Optional;

public interface AccountIbanRepository {
    List<AccountIban> findAll();
    Optional<Long> findAccountByIban(String iban);
    AccountIban save(CreateAccountIbanDto dto);
    void deleteById(long id);
}
```

- [ ] **Step 3: Create HbciSyncLogRepository**

```java
package de.flur4.roomiefunds.domain.spi;

public interface HbciSyncLogRepository {
    void saveLog(long accountId, int importedCount, int skippedCount, boolean success, String errorMessage);
}
```

- [ ] **Step 4: Create HbciClient**

```java
package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.hbci.DateRange;
import de.flur4.roomiefunds.models.hbci.HbciFetchResult;
import de.flur4.roomiefunds.models.hbci.HbciCredentials;

public interface HbciClient {
    HbciFetchResult fetchTransactions(HbciCredentials credentials, DateRange dateRange);
}
```

- [ ] **Step 5: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/domain/spi/HbciConfigRepository.java \
        src/main/java/de/flur4/roomiefunds/domain/spi/AccountIbanRepository.java \
        src/main/java/de/flur4/roomiefunds/domain/spi/HbciSyncLogRepository.java \
        src/main/java/de/flur4/roomiefunds/domain/spi/HbciClient.java
git commit -m "feat: add HBCI domain SPI interfaces"
```

---

## Task 4: Domain API Interfaces

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/domain/api/hbcisync/GetHbciConfig.java`
- Create: `src/main/java/de/flur4/roomiefunds/domain/api/hbcisync/SaveHbciConfig.java`
- Create: `src/main/java/de/flur4/roomiefunds/domain/api/hbcisync/SyncBankTransactions.java`

- [ ] **Step 1: Create GetHbciConfig**

```java
package de.flur4.roomiefunds.domain.api.hbcisync;

import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.HbciConfig;

import java.util.List;
import java.util.Optional;

public interface GetHbciConfig {
    List<HbciConfig> getConfigs();
    Optional<HbciConfig> getConfigByAccountId(long accountId);
    List<AccountIban> getIbans();
}
```

- [ ] **Step 2: Create SaveHbciConfig**

```java
package de.flur4.roomiefunds.domain.api.hbcisync;

import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;
import de.flur4.roomiefunds.models.hbci.HbciConfig;
import de.flur4.roomiefunds.models.hbci.SaveHbciConfigDto;

public interface SaveHbciConfig {
    HbciConfig createConfig(SaveHbciConfigDto dto);
    void updateConfig(long accountId, SaveHbciConfigDto dto);
    void deleteConfig(long accountId);
    AccountIban addIban(CreateAccountIbanDto dto);
    void deleteIban(long id);
}
```

- [ ] **Step 3: Create SyncBankTransactions**

```java
package de.flur4.roomiefunds.domain.api.hbcisync;

import de.flur4.roomiefunds.models.hbci.HbciSyncResult;

public interface SyncBankTransactions {
    HbciSyncResult sync(long accountId);
}
```

- [ ] **Step 4: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/domain/api/hbcisync/
git commit -m "feat: add HBCI domain API interfaces"
```

---

## Task 5: Extend TransactionRepository

**Files:**
- Modify: `src/main/java/de/flur4/roomiefunds/domain/spi/TransactionRepository.java`
- Modify: `src/main/java/de/flur4/roomiefunds/infrastructure/repository/TransactionRepositoryImpl.java`

- [ ] **Step 1: Add two methods to the SPI interface**

In `TransactionRepository.java`, add after `createTransaction`:

```java
boolean camtIdExists(String camtId);

Transaction createTransactionWithCamtId(CreateTransactionDto createTransactionDto, String camtId);
```

The full file after the addition:

```java
package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.transaction.CreateTransactionDto;
import de.flur4.roomiefunds.models.transaction.ReceiptDto;
import de.flur4.roomiefunds.models.transaction.Transaction;
import de.flur4.roomiefunds.models.transaction.UpdateTransactionDto;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    boolean accountHasTransactions(long accountId);

    Optional<Transaction> getTransactionById(long transactionId);

    List<Transaction> getTransactionsByAccountId(long accountId);

    void deleteTransaction(long transactionId);

    Transaction createTransaction(CreateTransactionDto createTransactionDto);

    Transaction createTransactionWithCamtId(CreateTransactionDto createTransactionDto, String camtId);

    boolean camtIdExists(String camtId);

    Transaction updateTransaction(long transactionId, UpdateTransactionDto updateTransactionDto);

    Optional<ReceiptDto> getTransactionReceipt(long transactionId);

    Transaction deleteTransactionReceipt(long transactionId);

    Transaction setTransactionReceipt(long transactionId, FileUpload fileUpload) throws IOException;
}
```

- [ ] **Step 2: Implement both methods in TransactionRepositoryImpl**

Add after the existing `createTransaction` method:

```java
@Override
public boolean camtIdExists(String camtId) {
    return jooq.select(exists(
            jooq.selectOne()
                    .from(TRANSACTION)
                    .where(TRANSACTION.CAMT_ID.eq(camtId))
    )).fetchOne().value1();
}

@Override
public Transaction createTransactionWithCamtId(CreateTransactionDto dto, String camtId) {
    long newTransactionId = jooq.insertInto(TRANSACTION).columns(
            TRANSACTION.SOURCE_ACCOUNT_ID,
            TRANSACTION.TARGET_ACCOUNT_ID,
            TRANSACTION.AMOUNT,
            TRANSACTION.VALUE_DATE,
            TRANSACTION.DESCRIPTION,
            TRANSACTION.CAMT_ID
    ).values(
            dto.sourceAccountId(),
            dto.targetAccountId(),
            dto.amount(),
            dto.valueDate(),
            dto.description(),
            camtId
    ).returningResult(TRANSACTION.ID).fetchOne().value1();
    return getTransactionById(newTransactionId).orElseThrow();
}
```

- [ ] **Step 3: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/domain/spi/TransactionRepository.java \
        src/main/java/de/flur4/roomiefunds/infrastructure/repository/TransactionRepositoryImpl.java
git commit -m "feat: extend TransactionRepository with CAMT ID support"
```

---

## Task 6: AesEncryptionService

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/infrastructure/hbci/AesEncryptionService.java`
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: Create AesEncryptionService**

```java
package de.flur4.roomiefunds.infrastructure.hbci;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@ApplicationScoped
public class AesEncryptionService {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    @ConfigProperty(name = "app.hbci.aes-key")
    String aesKeyBase64;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = Base64.getDecoder().decode(aesKeyBase64);
        secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        return encryptBytes(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public String encryptBytes(byte[] plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        return new String(decryptBytes(ciphertext), StandardCharsets.UTF_8);
    }

    public byte[] decryptBytes(String ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
```

- [ ] **Step 2: Add HBCI config to application.properties**

Add at the end of `src/main/resources/application.properties`:

```properties
# HBCI bank sync
app.hbci.aes-key=${HBCI_AES_KEY}
app.hbci.initial-lookback-days=90
```

`HBCI_AES_KEY` must be a base64-encoded 32-byte (256-bit) AES key. Generate one with:
`openssl rand -base64 32`

- [ ] **Step 3: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/infrastructure/hbci/AesEncryptionService.java \
        src/main/resources/application.properties
git commit -m "feat: add AES-256-GCM encryption service for HBCI credentials"
```

---

## Task 7: HbciConfigRepositoryImpl

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/infrastructure/repository/HbciConfigRepositoryImpl.java`

- [ ] **Step 1: Create HbciConfigRepositoryImpl**

All DB operations use `HBCI_CONFIG.ACCOUNT_ID` as the lookup column — the surrogate PK (`id`) is only used internally when inserting.

```java
package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.HbciConfigRepository;
import de.flur4.roomiefunds.infrastructure.hbci.AesEncryptionService;
import de.flur4.roomiefunds.models.hbci.HbciConfig;
import de.flur4.roomiefunds.models.hbci.HbciCredentials;
import de.flur4.roomiefunds.models.hbci.SaveHbciConfigDto;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.HBCI_CONFIG;
import static org.jooq.Records.mapping;

@ApplicationScoped
@RequiredArgsConstructor
public class HbciConfigRepositoryImpl implements HbciConfigRepository {
    final DSLContext jooq;
    final AesEncryptionService aes;

    @Override
    public List<HbciConfig> findAll() {
        return jooq.select(
                        HBCI_CONFIG.ID,
                        HBCI_CONFIG.BLZ,
                        HBCI_CONFIG.USERNAME,
                        HBCI_CONFIG.ACCOUNT_ID,
                        HBCI_CONFIG.LAST_SYNCED_AT
                ).from(HBCI_CONFIG)
                .orderBy(HBCI_CONFIG.ACCOUNT_ID)
                .fetch(mapping(HbciConfig::new));
    }

    @Override
    public Optional<HbciConfig> findByAccountId(long accountId) {
        return jooq.select(
                        HBCI_CONFIG.ID,
                        HBCI_CONFIG.BLZ,
                        HBCI_CONFIG.USERNAME,
                        HBCI_CONFIG.ACCOUNT_ID,
                        HBCI_CONFIG.LAST_SYNCED_AT
                ).from(HBCI_CONFIG)
                .where(HBCI_CONFIG.ACCOUNT_ID.eq(accountId))
                .fetchOptional(mapping(HbciConfig::new));
    }

    @Override
    public Optional<HbciCredentials> loadCredentials(long accountId) {
        return jooq.select(
                        HBCI_CONFIG.BLZ,
                        HBCI_CONFIG.USERNAME,
                        HBCI_CONFIG.ENCRYPTED_PIN,
                        HBCI_CONFIG.ENCRYPTED_PASSPORT,
                        HBCI_CONFIG.ACCOUNT_ID,
                        HBCI_CONFIG.LAST_SYNCED_AT
                ).from(HBCI_CONFIG)
                .where(HBCI_CONFIG.ACCOUNT_ID.eq(accountId))
                .fetchOptional(r -> new HbciCredentials(
                        r.value1(),
                        r.value2(),
                        aes.decrypt(r.value3()),
                        r.value4() != null ? aes.decryptBytes(r.value4()) : new byte[0],
                        r.value5(),
                        r.value6()
                ));
    }

    @Override
    public HbciConfig createConfig(SaveHbciConfigDto dto) {
        long newId = jooq.insertInto(HBCI_CONFIG)
                .columns(HBCI_CONFIG.BLZ, HBCI_CONFIG.USERNAME, HBCI_CONFIG.ENCRYPTED_PIN, HBCI_CONFIG.ACCOUNT_ID)
                .values(dto.blz(), dto.username(), aes.encrypt(dto.pin()), dto.accountId())
                .returningResult(HBCI_CONFIG.ID)
                .fetchOne().value1();
        return findByAccountId(dto.accountId()).orElseThrow();
    }

    @Override
    public void updateConfig(long accountId, SaveHbciConfigDto dto) {
        jooq.update(HBCI_CONFIG)
                .set(HBCI_CONFIG.BLZ, dto.blz())
                .set(HBCI_CONFIG.USERNAME, dto.username())
                .set(HBCI_CONFIG.ENCRYPTED_PIN, aes.encrypt(dto.pin()))
                .where(HBCI_CONFIG.ACCOUNT_ID.eq(accountId))
                .execute();
    }

    @Override
    public void deleteByAccountId(long accountId) {
        jooq.deleteFrom(HBCI_CONFIG)
                .where(HBCI_CONFIG.ACCOUNT_ID.eq(accountId))
                .execute();
    }

    @Override
    public void savePassportBytes(long accountId, byte[] passportBytes) {
        jooq.update(HBCI_CONFIG)
                .set(HBCI_CONFIG.ENCRYPTED_PASSPORT, aes.encryptBytes(passportBytes))
                .where(HBCI_CONFIG.ACCOUNT_ID.eq(accountId))
                .execute();
    }

    @Override
    public void updateLastSyncedAt(long accountId, OffsetDateTime syncedAt) {
        jooq.update(HBCI_CONFIG)
                .set(HBCI_CONFIG.LAST_SYNCED_AT, syncedAt)
                .where(HBCI_CONFIG.ACCOUNT_ID.eq(accountId))
                .execute();
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/infrastructure/repository/HbciConfigRepositoryImpl.java
git commit -m "feat: add HbciConfigRepositoryImpl with accountId-based lookup"
```

---

## Task 8: AccountIbanRepositoryImpl and HbciSyncLogRepositoryImpl

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/infrastructure/repository/AccountIbanRepositoryImpl.java`
- Create: `src/main/java/de/flur4/roomiefunds/infrastructure/repository/HbciSyncLogRepositoryImpl.java`

- [ ] **Step 1: Create AccountIbanRepositoryImpl**

```java
package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.AccountIbanRepository;
import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.ACCOUNT_IBAN;
import static org.jooq.Records.mapping;

@ApplicationScoped
@RequiredArgsConstructor
public class AccountIbanRepositoryImpl implements AccountIbanRepository {
    final DSLContext jooq;

    @Override
    public List<AccountIban> findAll() {
        return jooq.select(ACCOUNT_IBAN.ID, ACCOUNT_IBAN.ACCOUNT_ID, ACCOUNT_IBAN.IBAN)
                .from(ACCOUNT_IBAN)
                .orderBy(ACCOUNT_IBAN.ID)
                .fetch(mapping(AccountIban::new));
    }

    @Override
    public Optional<Long> findAccountByIban(String iban) {
        return jooq.select(ACCOUNT_IBAN.ACCOUNT_ID)
                .from(ACCOUNT_IBAN)
                .where(ACCOUNT_IBAN.IBAN.eq(iban))
                .fetchOptional(r -> r.value1());
    }

    @Override
    public AccountIban save(CreateAccountIbanDto dto) {
        long newId = jooq.insertInto(ACCOUNT_IBAN)
                .columns(ACCOUNT_IBAN.ACCOUNT_ID, ACCOUNT_IBAN.IBAN)
                .values(dto.accountId(), dto.iban())
                .returningResult(ACCOUNT_IBAN.ID)
                .fetchOne().value1();
        return jooq.select(ACCOUNT_IBAN.ID, ACCOUNT_IBAN.ACCOUNT_ID, ACCOUNT_IBAN.IBAN)
                .from(ACCOUNT_IBAN)
                .where(ACCOUNT_IBAN.ID.eq(newId))
                .fetchOne(mapping(AccountIban::new));
    }

    @Override
    public void deleteById(long id) {
        jooq.deleteFrom(ACCOUNT_IBAN).where(ACCOUNT_IBAN.ID.eq(id)).execute();
    }
}
```

- [ ] **Step 2: Create HbciSyncLogRepositoryImpl**

```java
package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.HbciSyncLogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.HBCI_SYNC_LOG;

@ApplicationScoped
@RequiredArgsConstructor
public class HbciSyncLogRepositoryImpl implements HbciSyncLogRepository {
    final DSLContext jooq;

    @Override
    public void saveLog(long accountId, int importedCount, int skippedCount, boolean success, String errorMessage) {
        jooq.insertInto(HBCI_SYNC_LOG)
                .columns(
                        HBCI_SYNC_LOG.ACCOUNT_ID,
                        HBCI_SYNC_LOG.IMPORTED_COUNT,
                        HBCI_SYNC_LOG.SKIPPED_COUNT,
                        HBCI_SYNC_LOG.SUCCESS,
                        HBCI_SYNC_LOG.ERROR_MESSAGE
                ).values(accountId, importedCount, skippedCount, success, errorMessage)
                .execute();
    }
}
```

- [ ] **Step 3: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/infrastructure/repository/AccountIbanRepositoryImpl.java \
        src/main/java/de/flur4/roomiefunds/infrastructure/repository/HbciSyncLogRepositoryImpl.java
git commit -m "feat: add AccountIban and HbciSyncLog repository implementations"
```

---

## Task 9: HbciClientImpl

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/infrastructure/hbci/HbciClientImpl.java`

- [ ] **Step 1: Create HbciClientImpl**

```java
package de.flur4.roomiefunds.infrastructure.hbci;

import de.flur4.roomiefunds.domain.spi.HbciClient;
import de.flur4.roomiefunds.models.hbci.DateRange;
import de.flur4.roomiefunds.models.hbci.HbciCredentials;
import de.flur4.roomiefunds.models.hbci.HbciFetchResult;
import de.flur4.roomiefunds.models.hbci.HbciSyncException;
import de.flur4.roomiefunds.models.hbci.HbciTransactionEntry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.kapott.hbci.GV.HBCIJob;
import org.kapott.hbci.GV_Result.HBCIJobResult;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIVersion;
import org.kapott.hbci.passport.AbstractHBCIPassport;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@ApplicationScoped
@Slf4j
public class HbciClientImpl implements HbciClient {
    private String currentBlz;
    private String currentUsername;
    private String currentPin;

    @PostConstruct
    void init() {
        HBCIUtils.init(new Properties(), new HbciCallback());
    }

    @Override
    public HbciFetchResult fetchTransactions(HbciCredentials credentials, DateRange dateRange) {
        this.currentBlz = credentials.blz();
        this.currentUsername = credentials.username();
        this.currentPin = credentials.pin();

        File passportFile = null;
        try {
            passportFile = File.createTempFile("hbci-passport-", ".dat");
            if (credentials.passportBytes() != null && credentials.passportBytes().length > 0) {
                Files.write(passportFile.toPath(), credentials.passportBytes());
            }

            HBCIUtils.setParam("client.passport.default", "PinTan");
            HBCIUtils.setParam("client.passport.PinTan.filename", passportFile.getAbsolutePath());
            HBCIUtils.setParam("client.passport.PinTan.init", "1");

            AbstractHBCIPassport passport = AbstractHBCIPassport.getInstance();
            passport.setCountry("DE");
            passport.setServer(HBCIUtils.getBankInfo(currentBlz).getPinTanAddress());
            passport.setPort(new Integer(443));
            passport.setFilterType("Base64");

            HBCIHandler handler = new HBCIHandler(HBCIVersion.HBCI300.getId(), passport);

            HBCIJob job = handler.newJob("KUmsAllCamt");
            job.setParam("my", passport.getAccounts()[0]);
            job.setParam("startdate", toDate(dateRange.from()));
            job.setParam("enddate", toDate(dateRange.to()));
            job.addToQueue();

            HBCIExecStatus status = handler.execute();
            handler.close();

            GVRKUms result = (GVRKUms) job.getJobResult();
            if (!result.isOK()) {
                throw new HbciSyncException("HBCI job failed: " + result.toString());
            }

            List<HbciTransactionEntry> entries = new ArrayList<>();
            for (GVRKUms.UmsLine line : result.getFlatData()) {
                if (line.id == null || line.id.isBlank()) {
                    log.warn("Skipping entry without CAMT ID on {}", line.bdate);
                    continue;
                }
                String iban = line.other != null ? line.other.iban : null;
                String usage = line.usage != null ? String.join(" ", line.usage) : "";
                LocalDate valueDate = line.valuta.toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                int amountCents = line.value.getBigDecimalValue()
                        .multiply(BigDecimal.valueOf(100)).intValue();
                entries.add(new HbciTransactionEntry(line.id, valueDate, amountCents, iban, usage));
            }

            byte[] updatedPassport = Files.readAllBytes(passportFile.toPath());
            return new HbciFetchResult(entries, updatedPassport);

        } catch (HbciSyncException e) {
            throw e;
        } catch (Exception e) {
            throw new HbciSyncException("HBCI fetch failed: " + e.getMessage(), e);
        } finally {
            if (passportFile != null) passportFile.delete();
        }
    }

    private Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private class HbciCallback extends AbstractHBCICallback {
        @Override
        public void callback(HBCIPassport passport, int reason, String msg,
                             int datatype, StringBuffer retData) {
            switch (reason) {
                case NEED_PASSPHRASE_LOAD, NEED_PASSPHRASE_SAVE ->
                        retData.replace(0, retData.length(), currentPin);
                case NEED_PT_PIN ->
                        retData.replace(0, retData.length(), currentPin);
                case NEED_BLZ ->
                        retData.replace(0, retData.length(), currentBlz);
                case NEED_USERID, NEED_CUSTOMERID ->
                        retData.replace(0, retData.length(), currentUsername);
                case NEED_PT_DECOUPLED ->
                        log.info("Waiting for phone approval...");
            }
        }

        @Override
        public void log(String msg, int level, Date date, StackTraceElement trace) {
            log.debug("HBCI: {}", msg);
        }

        @Override
        public void status(HBCIPassport passport, int statusTag, Object[] o) {}
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/infrastructure/hbci/HbciClientImpl.java
git commit -m "feat: add HbciClientImpl wrapping hbci4java"
```

---

## Task 10: HbciSyncService

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/domain/api/hbcisync/impl/HbciSyncService.java`

- [ ] **Step 1: Create HbciSyncService**

```java
package de.flur4.roomiefunds.domain.api.hbcisync.impl;

import de.flur4.roomiefunds.domain.api.hbcisync.GetHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SaveHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SyncBankTransactions;
import de.flur4.roomiefunds.domain.spi.AccountIbanRepository;
import de.flur4.roomiefunds.domain.spi.HbciClient;
import de.flur4.roomiefunds.domain.spi.HbciConfigRepository;
import de.flur4.roomiefunds.domain.spi.HbciSyncLogRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import de.flur4.roomiefunds.models.hbci.HbciConfig;
import de.flur4.roomiefunds.models.hbci.HbciSyncResult;
import de.flur4.roomiefunds.models.transaction.CreateTransactionDto;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HbciSyncService implements GetHbciConfig, SaveHbciConfig, SyncBankTransactions {
    private final HbciConfigRepository hbciConfigRepository;
    private final AccountIbanRepository accountIbanRepository;
    private final HbciSyncLogRepository syncLogRepository;
    private final HbciClient hbciClient;
    private final TransactionRepository transactionRepository;
    private final int initialLookbackDays;

    @Override
    public List<HbciConfig> getConfigs() {
        return hbciConfigRepository.findAll();
    }

    @Override
    public Optional<HbciConfig> getConfigByAccountId(long accountId) {
        return hbciConfigRepository.findByAccountId(accountId);
    }

    @Override
    public List<AccountIban> getIbans() {
        return accountIbanRepository.findAll();
    }

    @Override
    public HbciConfig createConfig(SaveHbciConfigDto dto) {
        return hbciConfigRepository.createConfig(dto);
    }

    @Override
    public void updateConfig(long accountId, SaveHbciConfigDto dto) {
        hbciConfigRepository.updateConfig(accountId, dto);
    }

    @Override
    public void deleteConfig(long accountId) {
        hbciConfigRepository.deleteByAccountId(accountId);
    }

    @Override
    public AccountIban addIban(CreateAccountIbanDto dto) {
        return accountIbanRepository.save(dto);
    }

    @Override
    public void deleteIban(long id) {
        accountIbanRepository.deleteById(id);
    }

    @Override
    public HbciSyncResult sync(long accountId) {
        var credentials = hbciConfigRepository.loadCredentials(accountId)
                .orElseThrow(() -> new HbciSyncException("No HBCI config found for account " + accountId));

        DateRange dateRange = buildDateRange(credentials.lastSyncedAt());

        HbciFetchResult fetchResult;
        try {
            fetchResult = hbciClient.fetchTransactions(credentials, dateRange);
        } catch (Exception e) {
            syncLogRepository.saveLog(accountId, 0, 0, false, e.getMessage());
            throw e instanceof HbciSyncException ? (HbciSyncException) e
                    : new HbciSyncException("HBCI fetch failed", e);
        }

        hbciConfigRepository.savePassportBytes(accountId, fetchResult.updatedPassportBytes());

        int imported = 0, skipped = 0;
        try {
            for (var entry : fetchResult.entries()) {
                if (entry.amountCents() <= 0) { skipped++; continue; }
                if (transactionRepository.camtIdExists(entry.camtId())) { skipped++; continue; }
                var sourceAccountId = accountIbanRepository.findAccountByIban(entry.counterpartyIban());
                if (sourceAccountId.isEmpty()) { skipped++; continue; }
                transactionRepository.createTransactionWithCamtId(
                        new CreateTransactionDto(sourceAccountId.get(), accountId,
                                entry.amountCents(), entry.valueDate(), entry.usage()),
                        entry.camtId());
                imported++;
            }
            hbciConfigRepository.updateLastSyncedAt(accountId, OffsetDateTime.now());
            syncLogRepository.saveLog(accountId, imported, skipped, true, null);
        } catch (Exception e) {
            syncLogRepository.saveLog(accountId, imported, skipped, false, e.getMessage());
            throw e;
        }
        return new HbciSyncResult(imported, skipped);
    }

    private DateRange buildDateRange(OffsetDateTime lastSyncedAt) {
        LocalDate to = LocalDate.now();
        LocalDate from = lastSyncedAt != null
                ? lastSyncedAt.toLocalDate().minusDays(3)
                : LocalDate.now().minusDays(initialLookbackDays);
        return new DateRange(from, to);
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/domain/api/hbcisync/impl/HbciSyncService.java
git commit -m "feat: add HbciSyncService"
```

---

## Task 11: HbciSyncContext Bootstrap

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/bootstrap/HbciSyncContext.java`

- [ ] **Step 1: Create HbciSyncContext**

```java
package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.hbcisync.GetHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SaveHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SyncBankTransactions;
import de.flur4.roomiefunds.domain.api.hbcisync.impl.HbciSyncService;
import de.flur4.roomiefunds.domain.spi.AccountIbanRepository;
import de.flur4.roomiefunds.domain.spi.HbciClient;
import de.flur4.roomiefunds.domain.spi.HbciConfigRepository;
import de.flur4.roomiefunds.domain.spi.HbciSyncLogRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class HbciSyncContext {
    @ConfigProperty(name = "app.hbci.initial-lookback-days", defaultValue = "90")
    int initialLookbackDays;

    @Produces
    @ApplicationScoped
    public GetHbciConfig getHbciConfig(HbciConfigRepository configRepo,
                                       AccountIbanRepository ibanRepo,
                                       HbciClient hbciClient,
                                       TransactionRepository txRepo,
                                       HbciSyncLogRepository logRepo) {
        return new HbciSyncService(configRepo, ibanRepo, logRepo, hbciClient, txRepo, initialLookbackDays);
    }

    @Produces
    @ApplicationScoped
    public SaveHbciConfig saveHbciConfig(HbciConfigRepository configRepo,
                                         AccountIbanRepository ibanRepo,
                                         HbciClient hbciClient,
                                         TransactionRepository txRepo,
                                         HbciSyncLogRepository logRepo) {
        return new HbciSyncService(configRepo, ibanRepo, logRepo, hbciClient, txRepo, initialLookbackDays);
    }

    @Produces
    @ApplicationScoped
    public SyncBankTransactions syncBankTransactions(HbciConfigRepository configRepo,
                                                      AccountIbanRepository ibanRepo,
                                                      HbciClient hbciClient,
                                                      TransactionRepository txRepo,
                                                      HbciSyncLogRepository logRepo) {
        return new HbciSyncService(configRepo, ibanRepo, logRepo, hbciClient, txRepo, initialLookbackDays);
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/bootstrap/HbciSyncContext.java
git commit -m "feat: add HbciSyncContext bootstrap"
```

---

## Task 12: HbciController

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/infrastructure/web/HbciController.java`

- [ ] **Step 1: Create HbciController**

The path parameter in all `/configs/{accountId}` routes is the internal account's ID, not the hbci_config surrogate key.

```java
package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.hbcisync.GetHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SaveHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SyncBankTransactions;
import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;
import de.flur4.roomiefunds.models.hbci.HbciConfig;
import de.flur4.roomiefunds.models.hbci.HbciSyncResult;
import de.flur4.roomiefunds.models.hbci.SaveHbciConfigDto;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Path("/api/hbci")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
@RequiredArgsConstructor
public class HbciController {
    final GetHbciConfig getHbciConfig;
    final SaveHbciConfig saveHbciConfig;
    final SyncBankTransactions syncBankTransactions;

    @GET
    @Path("/configs")
    public List<HbciConfig> getConfigs() {
        return getHbciConfig.getConfigs();
    }

    @POST
    @Path("/configs")
    public HbciConfig createConfig(SaveHbciConfigDto dto) {
        return saveHbciConfig.createConfig(dto);
    }

    @PUT
    @Path("/configs/{accountId}")
    public void updateConfig(@PathParam("accountId") long accountId, SaveHbciConfigDto dto) {
        saveHbciConfig.updateConfig(accountId, dto);
    }

    @DELETE
    @Path("/configs/{accountId}")
    public void deleteConfig(@PathParam("accountId") long accountId) {
        saveHbciConfig.deleteConfig(accountId);
    }

    @POST
    @Path("/configs/{accountId}/sync")
    public HbciSyncResult sync(@PathParam("accountId") long accountId) {
        return syncBankTransactions.sync(accountId);
    }

    @GET
    @Path("/ibans")
    public List<AccountIban> getIbans() {
        return getHbciConfig.getIbans();
    }

    @POST
    @Path("/ibans")
    public AccountIban addIban(CreateAccountIbanDto dto) {
        return saveHbciConfig.addIban(dto);
    }

    @DELETE
    @Path("/ibans/{id}")
    public void deleteIban(@PathParam("id") long id) {
        saveHbciConfig.deleteIban(id);
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/infrastructure/web/HbciController.java
git commit -m "feat: add HbciController"
```

---

## Task 13: End-to-End Smoke Check

- [ ] **Step 1: Start the application**

```bash
export HBCI_AES_KEY=$(openssl rand -base64 32)
./mvnw quarkus:dev
```

Expected: Application starts successfully with Flyway migrations applied (V16, V17).

- [ ] **Step 2: Verify endpoints are reachable**

```bash
curl -s http://localhost:8080/api/hbci/configs | jq .
```

Expected: Empty array `[]`

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: complete HBCI bank sync implementation"
```
