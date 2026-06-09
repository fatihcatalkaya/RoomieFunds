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
