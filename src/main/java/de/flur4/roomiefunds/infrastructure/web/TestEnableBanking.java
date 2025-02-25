package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.infrastructure.webclient.enablebanking.EnableBankingClient;
import de.flur4.roomiefunds.models.webclient.enablebanking.GetASPSPResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api/test")
public class TestEnableBanking {

    @RestClient
    EnableBankingClient enableBankingClient;

    @GET
    public GetASPSPResponse getASPSPs() {
        return enableBankingClient.getASPSPs("DE");
    }
}
