package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.models.config.OidcConfigurationDto;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Hands the SPA the OIDC parameters it needs to bootstrap oidc-spa.
 *
 * Deliberately unauthenticated: the frontend calls this before it has a token. Only values that are
 * public by nature (the issuer URI and the public client id) belong here.
 */
@Path("/api/config/oidc")
@PermitAll
@JBossLog
public class OidcConfigurationController {

    @ConfigProperty(name = "app.oidc.frontend.issuer-uri")
    String issuerUri;

    @ConfigProperty(name = "app.oidc.frontend.client-id")
    String clientId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OidcConfigurationDto getOidcConfiguration() {
        return new OidcConfigurationDto(issuerUri, clientId);
    }
}
