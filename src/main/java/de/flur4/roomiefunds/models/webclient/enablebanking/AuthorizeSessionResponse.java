package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AuthorizeSessionResponse(String sessionId,
                                       List<AccountResource> accounts,
                                       ASPSP aspsp,
                                       PSUType psuType,
                                       Access access) {
}
