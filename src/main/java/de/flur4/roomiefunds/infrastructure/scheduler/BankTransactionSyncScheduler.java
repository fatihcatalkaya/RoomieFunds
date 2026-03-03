package de.flur4.roomiefunds.infrastructure.scheduler;

import de.flur4.roomiefunds.domain.api.enablebanking.SyncBankTransactions;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
@RequiredArgsConstructor
public class BankTransactionSyncScheduler {
    final SyncBankTransactions syncBankTransactions;

    @Scheduled(cron = "${app.enablebanking.sync.cron:0 0 */12 * * ?}")
    void syncBankTransactions() {
        log.info("Starting scheduled bank transaction sync");
        try {
            var results = syncBankTransactions.syncAllSessions();
            for (var result : results) {
                if ("SUCCESS".equals(result.status())) {
                    log.infof("Session %d: fetched %d, inserted %d, deleted %d (overlap), balance match: %s",
                            result.sessionId(), result.transactionsFetched(), result.transactionsInserted(),
                            result.transactionsDeleted(), result.balanceMatch());
                } else if ("AUTH_REQUIRED".equals(result.status())) {
                    log.warnf("Session %d: authentication required — user needs to re-authenticate", result.sessionId());
                } else {
                    log.warnf("Session %d: sync failed — %s", result.sessionId(), result.errorMessage());
                }
            }
            log.infof("Finished bank transaction sync: %d sessions processed", results.size());
        } catch (Exception ex) {
            log.error("Failed to run bank transaction sync", ex);
        }
    }
}
