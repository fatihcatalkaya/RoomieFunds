package de.flur4.roomiefunds.infrastructure.webclient.enablebanking;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.flur4.roomiefunds.models.webclient.enablebanking.GetASPSPResponse;
import io.quarkus.rest.client.reactive.jackson.ClientObjectMapper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@RegisterProvider(EnableBankingAuthenticationInjector.class)
public interface EnableBankingClient {
    @GET
    @Path("/aspsps")
    GetASPSPResponse getASPSPs();

    /**
     * Get ASPSPs for a specific country. Country must be given as a single two-letter country code
     *
     * @param country two-letter country code
     * @return {@link GetASPSPResponse}
     */
    @GET
    @Path("/aspsps")
    GetASPSPResponse getASPSPs(@QueryParam("country") String country);

    @ClientObjectMapper
    static ObjectMapper objectMapper(ObjectMapper defaultObjectMapper) {
        return defaultObjectMapper.copy()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }
}
