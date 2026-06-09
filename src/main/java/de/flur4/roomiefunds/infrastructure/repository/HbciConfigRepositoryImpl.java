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
import java.util.NoSuchElementException;
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
        jooq.insertInto(HBCI_CONFIG)
                .columns(HBCI_CONFIG.BLZ, HBCI_CONFIG.USERNAME, HBCI_CONFIG.ENCRYPTED_PIN, HBCI_CONFIG.ACCOUNT_ID)
                .values(dto.blz(), dto.username(), aes.encrypt(dto.pin()), dto.accountId())
                .execute();
        return findByAccountId(dto.accountId()).orElseThrow();
    }

    @Override
    public void updateConfig(long accountId, SaveHbciConfigDto dto) {
        int updated = jooq.update(HBCI_CONFIG)
                .set(HBCI_CONFIG.BLZ, dto.blz())
                .set(HBCI_CONFIG.USERNAME, dto.username())
                .set(HBCI_CONFIG.ENCRYPTED_PIN, aes.encrypt(dto.pin()))
                .where(HBCI_CONFIG.ACCOUNT_ID.eq(accountId))
                .execute();
        if (updated == 0) throw new NoSuchElementException("No HBCI config for accountId=" + accountId);
    }

    @Override
    public void deleteByAccountId(long accountId) {
        int deleted = jooq.deleteFrom(HBCI_CONFIG)
                .where(HBCI_CONFIG.ACCOUNT_ID.eq(accountId))
                .execute();
        if (deleted == 0) throw new NoSuchElementException("No HBCI config for accountId=" + accountId);
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
