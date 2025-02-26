package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record StartAuthorizationResponse(
        /** URL to redirect PSU to */
        String url,

        /** PSU authorization ID, used to identify an authorization session */
        String authorizationId,

        /** Hashed unique identification of a PSU used by the client application */
        String psuIdHash
) {
}
