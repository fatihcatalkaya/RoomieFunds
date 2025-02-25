package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ASPSPData(@JsonProperty("name") String name,
                        @JsonProperty("country") String country,
                        @JsonProperty("logo") String logoUri,
                        @JsonProperty("psu_types") List<PSUType> psuTypes,
                        @JsonProperty("auth_methods") List<AuthMethod> authMethods,
                        @JsonProperty("maximum_consent_validity") Integer maximumConsentValidity,
                        @JsonProperty("beta") Boolean beta) {
}
