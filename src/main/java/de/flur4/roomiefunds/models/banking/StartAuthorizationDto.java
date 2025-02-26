package de.flur4.roomiefunds.models.banking;

import de.flur4.roomiefunds.models.webclient.enablebanking.ASPSP;

public record StartAuthorizationDto(ASPSP aspsp,
                                    long maximumConsentValidity,
                                    String authMethod) {
}
