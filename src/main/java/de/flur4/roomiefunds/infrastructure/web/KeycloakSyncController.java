package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.keycloaksync.FullKeycloakSync;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

@Path("/api/keycloak-sync")
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class KeycloakSyncController {
    final FullKeycloakSync fullKeycloakSync;

    @POST
    @Path("/full")
    public void fullSync() {
        try {
            fullKeycloakSync.fullSync();
        } catch (Exception e) {
            log.error("An error occurred during full Keycloak sync", e);
            throw new InternalServerErrorException("An error occurred during full Keycloak sync", e);
        }
    }
}
