package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record StartAuthorizationRequest(Access access,
                                        ASPSP aspsp,
                                        PSUType psuType,
                                        String authMethod,
                                        String language,
                                        String state,
                                        String redirectUrl,
                                        String psuId) {
}
