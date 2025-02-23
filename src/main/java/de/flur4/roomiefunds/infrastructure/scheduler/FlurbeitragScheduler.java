package de.flur4.roomiefunds.infrastructure.scheduler;

import de.flur4.roomiefunds.domain.api.flurbeitrag.CreateFlurbeitragTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@RequiredArgsConstructor
@JBossLog
public class FlurbeitragScheduler {

    final CreateFlurbeitragTransaction createFlurbeitragTransaction;

    @Scheduled(cron = "0 0 1 1 * ?")
    void createScheduledFlurbeitrag() {
        log.info("Starting creation of flurbeitrag transactions");
        try {
            final var result = createFlurbeitragTransaction.createFlurbeitragTransaction();
            switch (result) {
                case ALREADY_CREATED_IN_CURRENT_MONTH ->
                        log.info("Already created Flurbeitrag transactions for this month.");
                case FLURKONTO_NOT_CONFIGURED ->
                        log.error("The Flurkonto is not configured. Can not generate Flurbeitrag transactions.");
                case FLURBEITRAG_EQUALS_ZERO ->
                        log.info("The Flurbeitrag is set to zero euro. Not generating Flurbeitrag transactions.");
                case OK -> log.info("Finished creation of flurbeitrag transactions");
            }
        } catch (Exception ex) {
            log.error("Failed to create flurbeitrag transactions", ex);
        }
    }
}
