# HBCI Bank Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Import incoming bank payments automatically via HBCI/FinTS into RoomieFunds when an admin triggers a sync and approves the pushTAN on their phone.

**Architecture:** Admin-triggered long-poll REST endpoint calls `HbciSyncService`, which fetches transactions via `HbciClientImpl` (hbci4java), matches counterparty IBANs to internal accounts, and inserts transactions with CAMT IDs for deduplication. Credentials and passport state are stored AES-256-GCM encrypted in the database.

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
    DateRange.java
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
    last_synced_at      TIMESTAMPTZ
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
    synced_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    imported_count  INT NOT NULL,
    skipped_count   INT NOT NULL,
    success         BOOLEAN NOT NULL,
    error_message   TEXT
);
```

- [ ] **Step 2: Create V0017**

```sql
ALTER TABLE transaction ADD COLUMN camt_id TEXT UNIQUE;
```

- [ ] **Step 3: Regenerate jOOQ sources**

Run: `mvn generate-sources -Djooq-local`

Expected: BUILD SUCCESS. New jOOQ classes appear in `target/generated-sources/jooq/de/flur4/roomiefunds/infrastructure/jooq/tables/`: `HbciConfig.java`, `AccountIban.java`, `HbciSyncLog.java`. `Transaction.java` gains a `CAMT_ID` field.

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

```java
package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.hbci.HbciConfig;
import de.flur4.roomiefunds.models.hbci.HbciCredentials;
import de.flur4.roomiefunds.models.hbci.SaveHbciConfigDto;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface HbciConfigRepository {
    Optional<HbciConfig> getConfig();
    Optional<HbciCredentials> loadCredentials();
    void saveConfig(SaveHbciConfigDto dto);
    void savePassportBytes(byte[] passportBytes);
    void updateLastSyncedAt(OffsetDateTime syncedAt);
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
    void saveLog(int importedCount, int skippedCount, boolean success, String errorMessage);
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
    Optional<HbciConfig> getConfig();
    List<AccountIban> getIbans();
}
```

- [ ] **Step 2: Create SaveHbciConfig**

```java
package de.flur4.roomiefunds.domain.api.hbcisync;

import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;
import de.flur4.roomiefunds.models.hbci.SaveHbciConfigDto;

public interface SaveHbciConfig {
    void saveConfig(SaveHbciConfigDto dto);
    AccountIban addIban(CreateAccountIbanDto dto);
    void deleteIban(long id);
}
```

- [ ] **Step 3: Create SyncBankTransactions**

```java
package de.flur4.roomiefunds.domain.api.hbcisync;

import de.flur4.roomiefunds.models.hbci.HbciSyncResult;

public interface SyncBankTransactions {
    HbciSyncResult sync();
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

Add after the existing `createTransaction` method in `TransactionRepositoryImpl.java`:

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
public Transaction createTransactionWithCamtId(CreateTransactionDto createTransactionDto, String camtId) {
    long newTransactionId = jooq.insertInto(TRANSACTION).columns(
            TRANSACTION.SOURCE_ACCOUNT_ID,
            TRANSACTION.TARGET_ACCOUNT_ID,
            TRANSACTION.AMOUNT,
            TRANSACTION.VALUE_DATE,
            TRANSACTION.DESCRIPTION,
            TRANSACTION.CAMT_ID
    ).values(
            createTransactionDto.sourceAccountId(),
            createTransactionDto.targetAccountId(),
            createTransactionDto.amount(),
            createTransactionDto.valueDate(),
            createTransactionDto.description(),
            camtId
    ).returningResult(TRANSACTION.ID).fetchOne().value1();
    var transaction = getTransactionById(newTransactionId);
    assert transaction.isPresent();
    return transaction.get();
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
    private static final String ALGORITHM = "AES/GCM/NoPadding";
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
            Cipher cipher = Cipher.getInstance(ALGORITHM);
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
            Cipher cipher = Cipher.getInstance(ALGORITHM);
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
import java.util.Optional;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.HBCI_CONFIG;
import static org.jooq.Records.mapping;

@ApplicationScoped
@RequiredArgsConstructor
public class HbciConfigRepositoryImpl implements HbciConfigRepository {
    final DSLContext jooq;
    final AesEncryptionService aes;

    @Override
    public Optional<HbciConfig> getConfig() {
        return jooq.select(
                        HBCI_CONFIG.ID,
                        HBCI_CONFIG.BLZ,
                        HBCI_CONFIG.USERNAME,
                        HBCI_CONFIG.ACCOUNT_ID,
                        HBCI_CONFIG.LAST_SYNCED_AT
                ).from(HBCI_CONFIG)
                .limit(1)
                .fetchOptional(mapping(HbciConfig::new));
    }

    @Override
    public Optional<HbciCredentials> loadCredentials() {
        return jooq.select(
                        HBCI_CONFIG.BLZ,
                        HBCI_CONFIG.USERNAME,
                        HBCI_CONFIG.ENCRYPTED_PIN,
                        HBCI_CONFIG.ENCRYPTED_PASSPORT,
                        HBCI_CONFIG.ACCOUNT_ID,
                        HBCI_CONFIG.LAST_SYNCED_AT
                ).from(HBCI_CONFIG)
                .limit(1)
                .fetchOptional(r -> new HbciCredentials(
                        r.value1(),
                        r.value2(),
                        aes.decrypt(r.value3()),
                        r.value4() != null ? aes.decryptBytes(r.value4()) : new byte[0],
                        r.value5(),
                        r.value6() != null ? r.value6() : null
                ));
    }

    @Override
    public void saveConfig(SaveHbciConfigDto dto) {
        boolean exists = jooq.fetchExists(HBCI_CONFIG);
        if (exists) {
            jooq.update(HBCI_CONFIG)
                    .set(HBCI_CONFIG.BLZ, dto.blz())
                    .set(HBCI_CONFIG.USERNAME, dto.username())
                    .set(HBCI_CONFIG.ENCRYPTED_PIN, aes.encrypt(dto.pin()))
                    .set(HBCI_CONFIG.ACCOUNT_ID, dto.accountId())
                    .execute();
        } else {
            jooq.insertInto(HBCI_CONFIG)
                    .columns(HBCI_CONFIG.BLZ, HBCI_CONFIG.USERNAME, HBCI_CONFIG.ENCRYPTED_PIN, HBCI_CONFIG.ACCOUNT_ID)
                    .values(dto.blz(), dto.username(), aes.encrypt(dto.pin()), dto.accountId())
                    .execute();
        }
    }

    @Override
    public void savePassportBytes(byte[] passportBytes) {
        jooq.update(HBCI_CONFIG)
                .set(HBCI_CONFIG.ENCRYPTED_PASSPORT, aes.encryptBytes(passportBytes))
                .execute();
    }

    @Override
    public void updateLastSyncedAt(OffsetDateTime syncedAt) {
        jooq.update(HBCI_CONFIG)
                .set(HBCI_CONFIG.LAST_SYNCED_AT, syncedAt)
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
git commit -m "feat: add HbciConfigRepositoryImpl with AES encryption"
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
    public void saveLog(int importedCount, int skippedCount, boolean success, String errorMessage) {
        jooq.insertInto(HBCI_SYNC_LOG)
                .columns(
                        HBCI_SYNC_LOG.IMPORTED_COUNT,
                        HBCI_SYNC_LOG.SKIPPED_COUNT,
                        HBCI_SYNC_LOG.SUCCESS,
                        HBCI_SYNC_LOG.ERROR_MESSAGE
                ).values(importedCount, skippedCount, success, errorMessage)
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
import lombok.extern.jbosslog.JBossLog;
import org.kapott.hbci.GV.HBCIJob;
import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.callback.AbstractHBCICallback;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.BankInfo;
import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIVersion;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.status.HBCIExecStatus;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@ApplicationScoped
@JBossLog
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
        this.currentPin = credentials.decryptedPin();

        File passportFile = null;
        try {
            passportFile = File.createTempFile("hbci-passport-", ".dat");
            if (credentials.passportBytes() != null && credentials.passportBytes().length > 0) {
                Files.write(passportFile.toPath(), credentials.passportBytes());
            }

            HBCIUtils.setParam("client.passport.default", "PinTan");
            HBCIUtils.setParam("client.passport.PinTan.init", "1");

            HBCIPassport passport = AbstractHBCIPassport.getInstance(passportFile);
            passport.setCountry("DE");
            BankInfo info = HBCIUtils.getBankInfo(currentBlz);
            passport.setHost(info.getPinTanAddress());
            passport.setPort(443);
            passport.setFilterType("Base64");

            HBCIHandler handle = null;
            try {
                handle = new HBCIHandler(HBCIVersion.HBCI_300.getId(), passport);

                Date startDate = toDate(dateRange.from());
                Date endDate = toDate(dateRange.to());

                HBCIJob umsatzJob = handle.newJob("KUmsAllCamt");
                umsatzJob.setParam("my", passport.getAccounts()[0]);
                umsatzJob.setParam("startdate", startDate);
                umsatzJob.setParam("enddate", endDate);
                umsatzJob.addToQueue();

                HBCIExecStatus status = handle.execute();
                if (!status.isOK()) {
                    throw new HbciSyncException("HBCI execution failed: " + status.toString());
                }

                GVRKUms result = (GVRKUms) umsatzJob.getJobResult();
                if (!result.isOK()) {
                    throw new HbciSyncException("HBCI transaction fetch failed: " + result.toString());
                }

                List<HbciTransactionEntry> entries = new ArrayList<>();
                for (GVRKUms.UmsLine buchung : result.getFlatData()) {
                    if (buchung.id == null || buchung.id.isBlank()) {
                        log.warnf("Skipping HBCI entry with no CAMT ID on %s", buchung.valuta);
                        continue;
                    }
                    int amountCents = buchung.value != null
                            ? buchung.value.getBigDecimalValue().multiply(BigDecimal.valueOf(100)).intValue()
                            : 0;
                    String iban = (buchung.other != null) ? buchung.other.iban : null;
                    String usage = (buchung.usage != null && !buchung.usage.isEmpty())
                            ? String.join(" ", buchung.usage)
                            : "";
                    java.time.LocalDate valueDate = buchung.valuta.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                    entries.add(new HbciTransactionEntry(buchung.id, valueDate, amountCents, iban, usage));
                }

                byte[] updatedPassport = Files.readAllBytes(passportFile.toPath());
                return new HbciFetchResult(entries, updatedPassport);
            } finally {
                if (handle != null) handle.close();
                passport.close();
            }
        } catch (HbciSyncException e) {
            throw e;
        } catch (HBCI_Exception e) {
            throw new HbciSyncException("HBCI communication failed: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new HbciSyncException("Passport I/O failed: " + e.getMessage(), e);
        } finally {
            if (passportFile != null) passportFile.delete();
        }
    }

    private Date toDate(java.time.LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
    }

    private class HbciCallback extends AbstractHBCICallback {
        @Override
        public void log(String msg, int level, Date date, StackTraceElement trace) {
            log.debugf("HBCI: %s", msg);
        }

        @Override
        public void callback(HBCIPassport passport, int reason, String msg, int datatype, StringBuffer retData) {
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
                        log.info("HBCI: Waiting for pushTAN approval on phone...");
                case HAVE_ERROR ->
                        log.errorf("HBCI error: %s", msg);
                default -> { }
            }
        }

        @Override
        public void status(HBCIPassport passport, int statusTag, Object[] o) {
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS. Fix any import issues.

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
import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;
import de.flur4.roomiefunds.models.hbci.DateRange;
import de.flur4.roomiefunds.models.hbci.HbciConfig;
import de.flur4.roomiefunds.models.hbci.HbciFetchResult;
import de.flur4.roomiefunds.models.hbci.HbciSyncException;
import de.flur4.roomiefunds.models.hbci.HbciSyncResult;
import de.flur4.roomiefunds.models.hbci.HbciTransactionEntry;
import de.flur4.roomiefunds.models.hbci.SaveHbciConfigDto;
import de.flur4.roomiefunds.models.transaction.CreateTransactionDto;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class HbciSyncService implements GetHbciConfig, SaveHbciConfig, SyncBankTransactions {
    private final HbciConfigRepository configRepository;
    private final AccountIbanRepository ibanRepository;
    private final HbciSyncLogRepository logRepository;
    private final HbciClient hbciClient;
    private final TransactionRepository transactionRepository;
    private final int initialLookbackDays;

    @Override
    public Optional<HbciConfig> getConfig() {
        return configRepository.getConfig();
    }

    @Override
    public List<AccountIban> getIbans() {
        return ibanRepository.findAll();
    }

    @Override
    public void saveConfig(SaveHbciConfigDto dto) {
        configRepository.saveConfig(dto);
    }

    @Override
    public AccountIban addIban(CreateAccountIbanDto dto) {
        return ibanRepository.save(dto);
    }

    @Override
    public void deleteIban(long id) {
        ibanRepository.deleteById(id);
    }

    @Override
    public HbciSyncResult sync() {
        var credentials = configRepository.loadCredentials()
                .orElseThrow(() -> new HbciSyncException("No HBCI configuration found"));

        DateRange dateRange = buildDateRange(credentials.lastSyncedAt());

        HbciFetchResult fetchResult;
        try {
            fetchResult = hbciClient.fetchTransactions(credentials, dateRange);
        } catch (HbciSyncException e) {
            logRepository.saveLog(0, 0, false, e.getMessage());
            throw e;
        }

        configRepository.savePassportBytes(fetchResult.updatedPassportBytes());

        int imported = 0;
        int skipped = 0;

        try {
            for (HbciTransactionEntry entry : fetchResult.entries()) {
                if (entry.amountCents() <= 0) {
                    skipped++;
                    continue;
                }
                if (transactionRepository.camtIdExists(entry.camtId())) {
                    skipped++;
                    continue;
                }
                Optional<Long> sourceAccountId = ibanRepository.findAccountByIban(entry.counterpartyIban());
                if (sourceAccountId.isEmpty()) {
                    skipped++;
                    continue;
                }
                transactionRepository.createTransactionWithCamtId(
                        new CreateTransactionDto(
                                sourceAccountId.get(),
                                credentials.accountId(),
                                entry.amountCents(),
                                entry.valueDate(),
                                entry.usage()
                        ),
                        entry.camtId()
                );
                imported++;
            }

            configRepository.updateLastSyncedAt(OffsetDateTime.now());
            logRepository.saveLog(imported, skipped, true, null);
        } catch (Exception e) {
            logRepository.saveLog(imported, skipped, false, e.getMessage());
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
git add src/main/java/de/flur4/roomiefunds/domain/api/hbcisync/
git commit -m "feat: add HbciSyncService implementing HBCI import logic"
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
    public GetHbciConfig getHbciConfig(HbciConfigRepository configRepository,
                                       AccountIbanRepository ibanRepository,
                                       HbciSyncLogRepository logRepository,
                                       HbciClient hbciClient,
                                       TransactionRepository transactionRepository) {
        return new HbciSyncService(configRepository, ibanRepository, logRepository, hbciClient, transactionRepository, initialLookbackDays);
    }

    @Produces
    @ApplicationScoped
    public SaveHbciConfig saveHbciConfig(HbciConfigRepository configRepository,
                                         AccountIbanRepository ibanRepository,
                                         HbciSyncLogRepository logRepository,
                                         HbciClient hbciClient,
                                         TransactionRepository transactionRepository) {
        return new HbciSyncService(configRepository, ibanRepository, logRepository, hbciClient, transactionRepository, initialLookbackDays);
    }

    @Produces
    @ApplicationScoped
    public SyncBankTransactions syncBankTransactions(HbciConfigRepository configRepository,
                                                     AccountIbanRepository ibanRepository,
                                                     HbciSyncLogRepository logRepository,
                                                     HbciClient hbciClient,
                                                     TransactionRepository transactionRepository) {
        return new HbciSyncService(configRepository, ibanRepository, logRepository, hbciClient, transactionRepository, initialLookbackDays);
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/bootstrap/HbciSyncContext.java
git commit -m "feat: add HbciSyncContext CDI bootstrap"
```

---

## Task 12: HbciController

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/infrastructure/web/HbciController.java`

- [ ] **Step 1: Create HbciController**

```java
package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.hbcisync.GetHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SaveHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SyncBankTransactions;
import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;
import de.flur4.roomiefunds.models.hbci.HbciConfig;
import de.flur4.roomiefunds.models.hbci.HbciSyncException;
import de.flur4.roomiefunds.models.hbci.HbciSyncResult;
import de.flur4.roomiefunds.models.hbci.SaveHbciConfigDto;
import io.quarkus.cache.CacheInvalidateAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

import java.util.List;

@Path("/api/hbci")
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class HbciController {
    final GetHbciConfig getHbciConfig;
    final SaveHbciConfig saveHbciConfig;
    final SyncBankTransactions syncBankTransactions;

    @GET
    @Path("/config")
    public HbciConfig getConfig() {
        return getHbciConfig.getConfig()
                .orElseThrow(() -> new NotFoundException("No HBCI configuration found"));
    }

    @PUT
    @Path("/config")
    public void saveConfig(@Valid SaveHbciConfigDto dto) {
        saveHbciConfig.saveConfig(dto);
    }

    @GET
    @Path("/config/ibans")
    public List<AccountIban> getIbans() {
        return getHbciConfig.getIbans();
    }

    @POST
    @Path("/config/ibans")
    public AccountIban addIban(@Valid CreateAccountIbanDto dto) {
        return saveHbciConfig.addIban(dto);
    }

    @DELETE
    @Path("/config/ibans/{id:\\d+}")
    public void deleteIban(@PathParam("id") long id) {
        saveHbciConfig.deleteIban(id);
    }

    @POST
    @Path("/sync")
    @CacheInvalidateAll(cacheName = "accounts-with-balances")
    public HbciSyncResult sync() {
        try {
            return syncBankTransactions.sync();
        } catch (HbciSyncException e) {
            log.errorf("HBCI sync failed: %s", e.getMessage());
            throw new jakarta.ws.rs.InternalServerErrorException(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during HBCI sync", e);
            throw new jakarta.ws.rs.InternalServerErrorException("Sync failed unexpectedly", e);
        }
    }
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/flur4/roomiefunds/infrastructure/web/HbciController.java
git commit -m "feat: add HbciController REST endpoints"
```

---

## Task 13: End-to-End Compile and Smoke Check

- [ ] **Step 1: Full compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS with no warnings about unimplemented interfaces.

- [ ] **Step 2: Start the app (with HBCI_AES_KEY set)**

Generate a test key and start the app:

```bash
export HBCI_AES_KEY=$(openssl rand -base64 32)
./mvnw quarkus:dev -Dquarkus.http.port=8080
```

Expected: App starts. No CDI ambiguity errors. Flyway runs V0016 and V0017. Check logs for `Migrated to version 17`.

- [ ] **Step 3: Verify endpoints register**

```bash
curl -s http://localhost:8080/q/openapi | grep '/api/hbci'
```

Expected: Paths `/api/hbci/config`, `/api/hbci/config/ibans`, `/api/hbci/sync` appear.

- [ ] **Step 4: Commit final check**

```bash
git add -p  # verify nothing unintended is staged
git commit -m "feat: HBCI bank sync feature complete"
```
