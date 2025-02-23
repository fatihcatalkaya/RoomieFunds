package de.flur4.roomiefunds.infrastructure.scheduler;

import de.flur4.roomiefunds.domain.api.recurringtransaction.CreateRecurringTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@JBossLog
@RequiredArgsConstructor
public class RecurringTransactionScheduler {
    final CreateRecurringTransaction createRecurringTransaction;

    @Scheduled(cron = "0 30 1 1 * ?")
    void createScheduledTransactions() {
        log.info("Starting creation of scheduled transactions");
        try {
            createRecurringTransaction.createScheduledTransactions();
            log.info("Finished creation of scheduled transactions");
        } catch (Exception ex) {
            log.error("Failed to create flurbeitrag transactions", ex);
        }
    }
}
