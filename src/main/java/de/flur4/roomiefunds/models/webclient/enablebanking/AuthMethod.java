package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AuthMethod(
        String name,
        String title,
        PSUType psuType,
        List<Credential> credentials,
        AuthenticationApproach approach,
        boolean hiddenMethod
) {
}
