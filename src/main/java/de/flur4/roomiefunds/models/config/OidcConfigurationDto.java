package de.flur4.roomiefunds.models.config;

/**
 * Public OIDC parameters the browser needs in order to talk to the identity provider.
 * Everything in here ends up in the SPA at runtime, so it must never carry a secret.
 */
public record OidcConfigurationDto(String issuerUri, String clientId) {
}
