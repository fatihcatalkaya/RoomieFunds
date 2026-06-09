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
