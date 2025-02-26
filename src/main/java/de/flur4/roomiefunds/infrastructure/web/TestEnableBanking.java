package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.infrastructure.webclient.enablebanking.EnableBankingClient;
import de.flur4.roomiefunds.models.banking.StartAuthorizationDto;
import de.flur4.roomiefunds.models.webclient.enablebanking.*;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.jooq.tools.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;

@Path("/api/test")
@JBossLog
public class TestEnableBanking {

    @RestClient
    EnableBankingClient enableBankingClient;

    @GET
    public GetASPSPResponse getASPSPs() {
        return enableBankingClient.getASPSPs("DE");
    }

    @POST
    public StartAuthorizationResponse startAuthorization(StartAuthorizationDto dto) {
        var accessValidUntil = OffsetDateTime.now().plusSeconds(dto.maximumConsentValidity());
        StartAuthorizationRequest request = new StartAuthorizationRequest(
                new Access(null, null, null, accessValidUntil),
                dto.aspsp(),
                PSUType.PERSONAL,
                dto.authMethod(),
                "de",
                "",
                "http://100.124.16.54:8080/api/test/end", // TODO
                ""
        );
        var response = enableBankingClient.initializeSessionAuthPost(request);
        log.info(response);
        return response;
    }

    @GET
    @Path("/end")
    public RestResponse<Object> endAuthorization(@QueryParam("code") String code) throws URISyntaxException {
        if (StringUtils.isEmpty(code)) {
            // Authorization failed, we redirect the client to the post page with status code failed
            return RestResponse.ResponseBuilder
                    .temporaryRedirect(new URI("http://localhost:5173/app/banking/post-auth/?state=failed"))
                    .build();
        }

        var response = enableBankingClient.authorizeSessionSessionsPost(new AuthorizeSessionRequest(code));
        log.info(response);

        var response2 = enableBankingClient.getAccountBalancesByAccountId("4500105173424724776");

        return RestResponse.ResponseBuilder
                .temporaryRedirect(new URI("http://localhost:5173/app/banking/post-auth/?state=success"))
                .build();

    }

    @GET
    @Path("/stuff")
    public HalBalances getStuff() {
        return enableBankingClient.getAccountBalancesByAccountId("6e68386c-4347-4bbf-abaa-cb815ccc1ec9");
    }
}
