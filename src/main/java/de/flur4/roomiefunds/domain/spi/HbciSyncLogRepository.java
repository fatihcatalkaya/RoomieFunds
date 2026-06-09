package de.flur4.roomiefunds.domain.spi;

public interface HbciSyncLogRepository {
    void saveLog(long accountId, int importedCount, int skippedCount, boolean success, String errorMessage);
}
