package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.enablebanking.*;
import de.flur4.roomiefunds.infrastructure.Utils;
import de.flur4.roomiefunds.infrastructure.webclient.enablebanking.EnableBankingClient;
import de.flur4.roomiefunds.models.banking.StartAuthorizationDto;
import de.flur4.roomiefunds.models.enablebanking.BankTransactionsResult;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingSession;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingUnfinishedSession;
import de.flur4.roomiefunds.models.enablebanking.FinishSessionRequest;
import de.flur4.roomiefunds.models.webclient.enablebanking.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.jooq.tools.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Path("/api/enablebanking")
@JBossLog
@RequiredArgsConstructor
public class EnableBankingController {

    private static final Map<String, OffsetDateTime> stateTokens = new ConcurrentHashMap<>();

    @ConfigProperty(name = "app.backend.baseurl")
    String backendBaseUrl;

    @ConfigProperty(name = "app.frontend.baseurl")
    String frontendBaseUrl;

    @RestClient
    EnableBankingClient enableBankingClient;

    final StartAuthorization startAuthorization;
    final GetSession getSession;
    final FinishSession finishSession;
    final DeleteSession deleteSession;
    final GetBankTransactions getBankTransactions;
    final JsonWebToken jwt;

    @GET
    @RolesAllowed({"roomiefunds-admin"})
    public GetASPSPResponse getASPSPs(@QueryParam("country") @DefaultValue("DE") String country) {
        return enableBankingClient.getASPSPs(country);
    }

    @POST
    @RolesAllowed({"roomiefunds-admin"})
    public StartAuthorizationResponse startAuthorization(StartAuthorizationDto dto) {
        cleanExpiredStateTokens();
        var stateToken = UUID.randomUUID().toString();
        stateTokens.put(stateToken, OffsetDateTime.now().plusMinutes(15));

        var accessValidUntil = OffsetDateTime.now().plusSeconds(dto.maximumConsentValidity());
        StartAuthorizationRequest request = new StartAuthorizationRequest(
                new Access(null, null, null, accessValidUntil),
                dto.aspsp(),
                PSUType.PERSONAL,
                dto.authMethod(),
                "de",
                stateToken,
                backendBaseUrl + "/api/enablebanking/end",
                ""
        );
        return enableBankingClient.initializeSessionAuthPost(request);
    }

    @GET
    @Path("/end")
    public RestResponse<Object> endAuthorization(@QueryParam("code") String code, @QueryParam("state") String state) throws URISyntaxException {
        cleanExpiredStateTokens();

        if (StringUtils.isEmpty(state) || !stateTokens.containsKey(state)) {
            log.warn("Invalid or missing state token in OAuth callback");
            return RestResponse.ResponseBuilder
                    .temporaryRedirect(new URI(frontendBaseUrl + "/app/banking/post-auth/?state=failed"))
                    .build();
        }
        stateTokens.remove(state);

        if (StringUtils.isEmpty(code)) {
            return RestResponse.ResponseBuilder
                    .temporaryRedirect(new URI(frontendBaseUrl + "/app/banking/post-auth/?state=failed"))
                    .build();
        }

        var response = enableBankingClient.authorizeSessionSessionsPost(new AuthorizeSessionRequest(code));
        startAuthorization.completeAuthorization(response);

        return RestResponse.ResponseBuilder
                .temporaryRedirect(new URI(frontendBaseUrl + "/app/banking/post-auth/?state=success"))
                .build();
    }

    @GET
    @Path("/session")
    @RolesAllowed({"roomiefunds-admin"})
    public List<EnableBankingSession> getAllSessions() {
        return getSession.getAllSessions();
    }

    @GET
    @Path("/session/unfinished/{sessionId:\\d+}")
    @RolesAllowed({"roomiefunds-admin"})
    public EnableBankingUnfinishedSession getUnfinishedSessions(@PathParam("sessionId") long sessionId) {
        var result = getSession.getUnfinishedSession(sessionId);
        if (result.isEmpty()) {
            throw new NotFoundException();
        }
        return result.get();
    }

    @POST
    @Path("/session/unfinished/{sessionId:\\d+}")
    @RolesAllowed({"roomiefunds-admin"})
    public EnableBankingSession finishUnfinishedSession(@PathParam("sessionId") long sessionId, @Valid FinishSessionRequest dto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return finishSession.finishUnfinishedSession(modifyingPerson, sessionId, dto);
        } catch (SessionNotFoundException e) {
            throw new NotFoundException("Could not find session with id " + sessionId);
        } catch (SessionAlreadyFinishedException ex) {
            throw new BadRequestException(ex.getMessage());
        } catch (Exception e) {
            log.error(e);
            throw new InternalServerErrorException("An internal server error occurred");
        }
    }

    @DELETE
    @Path("/unfinished-session/{sessionId:\\d+}")
    @RolesAllowed({"roomiefunds-admin"})
    public void deleteUnfinishedSession(@PathParam("sessionId") long sessionId) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            deleteSession.deleteSession(modifyingPerson, sessionId);
        } catch (SessionNotFoundException e) {
            throw new NotFoundException("Could not find session with id " + sessionId);
        } catch (EnableBankingClientException e) {
            log.error(e.getMessage());
            throw new InternalServerErrorException("An error occurred while contacting EnableBanking");
        } catch (Exception e) {
            log.error(e);
            throw new InternalServerErrorException("An internal server error occurred");
        }
    }

    @GET
    @Path("/session/{sessionId:\\d+}/transactions")
    @RolesAllowed({"roomiefunds-admin"})
    public BankTransactionsResult getTransactions(
            @PathParam("sessionId") long sessionId,
            @QueryParam("dateFrom") LocalDate dateFrom,
            @QueryParam("dateTo") LocalDate dateTo) {
        try {
            return getBankTransactions.getBankTransactions(sessionId, dateFrom, dateTo);
        } catch (SessionNotFoundException e) {
            throw new NotFoundException("Could not find session with id " + sessionId);
        } catch (SessionExpiredException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private void cleanExpiredStateTokens() {
        var now = OffsetDateTime.now();
        Iterator<Map.Entry<String, OffsetDateTime>> it = stateTokens.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isBefore(now)) {
                it.remove();
            }
        }
    }
}
