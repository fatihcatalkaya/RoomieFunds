package de.flur4.roomiefunds.infrastructure.scheduler;

import de.flur4.roomiefunds.domain.api.keycloaksync.FullKeycloakSync;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

@ApplicationScoped
@RequiredArgsConstructor
@JBossLog
public class KeycloakSyncScheduler {

    final FullKeycloakSync fullKeycloakSync;

    @Scheduled(cron = "0 0 3 * * ?")
    void scheduledKeycloakSync() {
        log.info("Starting scheduled Keycloak sync");
        try {
            fullKeycloakSync.fullSync();
            log.info("Finished scheduled Keycloak sync");
        } catch (Exception e) {
            log.error("Failed scheduled Keycloak sync", e);
        }
    }
}
