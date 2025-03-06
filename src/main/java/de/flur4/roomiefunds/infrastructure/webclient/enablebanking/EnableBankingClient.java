package de.flur4.roomiefunds.infrastructure.webclient.enablebanking;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.EnumFeature;
import de.flur4.roomiefunds.models.webclient.enablebanking.*;
import io.quarkus.cache.CacheResult;
import io.quarkus.rest.client.reactive.jackson.ClientObjectMapper;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.LocalDate;

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
    @CacheResult(cacheName = "enablebanking-getaspsps")
    GetASPSPResponse getASPSPs(@QueryParam("country") String country);

    @POST
    @Path("/auth")
    StartAuthorizationResponse initializeSessionAuthPost(StartAuthorizationRequest request);

    @POST
    @Path("/sessions")
    AuthorizeSessionResponse authorizeSessionSessionsPost(AuthorizeSessionRequest request);

    @GET
    @Path("/accounts/{account_id}/balances")
    HalBalances getAccountBalancesByAccountId(@PathParam("account_id") String uuid);

    @DELETE
    @Path("/sessions/{session_id}")
    SuccessResponse deleteSession(@PathParam("session_id") String sessionId);

    /**
     * Fetching account transactions from ASPSP for an account by its ID
     *
     * @param uuid PSU account ID accessible in the provided session
     * @param dateFromUtc Date to fetch transactions from (including the date, UTC timezone is assumed)
     * @param dateToUtc Date to fetch transactions to (including the date, UTC timezone is assumed)
     * @param continuationKey Key, allowing to iterate over multiple API pages of transactions
     * @param transactionStatus Filter transactions by provided status
     * @param fetchStrategy Strategy how transaction are fetched
     * @return {@link HalTransactions}
     */
    @GET
    @Path("/accounts/{account_id}/transactions")
    HalTransactions getAccountTransactions(@PathParam("account_id") String uuid,
                                           LocalDate dateFromUtc,
                                           LocalDate dateToUtc,
                                           String continuationKey,
                                           TransactionStatus transactionStatus,
                                           TransactionFetchStrategy fetchStrategy);

    @ClientObjectMapper
    static ObjectMapper objectMapper(ObjectMapper defaultObjectMapper) {
        return defaultObjectMapper.copy()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(EnumFeature.WRITE_ENUMS_TO_LOWERCASE, true)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }
}
