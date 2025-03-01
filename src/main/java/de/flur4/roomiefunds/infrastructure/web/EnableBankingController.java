package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.enablebanking.*;
import de.flur4.roomiefunds.infrastructure.Utils;
import de.flur4.roomiefunds.infrastructure.repository.EnableBankingRepositoryImpl;
import de.flur4.roomiefunds.infrastructure.webclient.enablebanking.EnableBankingClient;
import de.flur4.roomiefunds.models.banking.StartAuthorizationDto;
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
import java.time.OffsetDateTime;
import java.util.List;

@Path("/api/enablebanking")
@JBossLog
@RequiredArgsConstructor
public class EnableBankingController {

    @ConfigProperty(name = "app.backend.baseurl")
    String backendBaseUrl;

    @ConfigProperty(name = "app.frontend.baseurl")
    String frontendBaseUrl;

    @RestClient
    EnableBankingClient enableBankingClient;

    @Inject
    EnableBankingRepositoryImpl enableBankingRepository;

    final GetSession getSession;
    final FinishSession finishSession;
    final DeleteSession deleteSession;
    final JsonWebToken jwt;

    @GET
    @RolesAllowed({"roomiefunds-admin"})
    public GetASPSPResponse getASPSPs() {
        return enableBankingClient.getASPSPs("DE");
    }

    @POST
    @RolesAllowed({"roomiefunds-admin"})
    public StartAuthorizationResponse startAuthorization(StartAuthorizationDto dto) {
        var accessValidUntil = OffsetDateTime.now().plusSeconds(dto.maximumConsentValidity());
        StartAuthorizationRequest request = new StartAuthorizationRequest(
                new Access(null, null, null, accessValidUntil),
                dto.aspsp(),
                PSUType.PERSONAL,
                dto.authMethod(),
                "de",
                "",
                backendBaseUrl + "/api/enablebanking/end",
                ""
        );
        return enableBankingClient.initializeSessionAuthPost(request);
    }

    @GET
    @Path("/end")
    public RestResponse<Object> endAuthorization(@QueryParam("code") String code) throws URISyntaxException {
        if (StringUtils.isEmpty(code)) {
            // Authorization failed, we redirect the client to the post page with status code failed
            return RestResponse.ResponseBuilder
                    .temporaryRedirect(new URI(frontendBaseUrl + "/app/banking/post-auth/?state=failed"))
                    .build();
        }

        // Authorization was successful, try to acquire a session token from EnableBanking
        var response = enableBankingClient.authorizeSessionSessionsPost(new AuthorizeSessionRequest(code));

        // Got session token, now we have to store it in our DB
        enableBankingRepository.storeNewSession(response);

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
}
