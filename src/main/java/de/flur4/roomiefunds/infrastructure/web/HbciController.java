package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.hbcisync.GetHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SaveHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SyncBankTransactions;
import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;
import de.flur4.roomiefunds.models.hbci.HbciConfig;
import de.flur4.roomiefunds.models.hbci.HbciSyncException;
import de.flur4.roomiefunds.models.hbci.HbciSyncResult;
import de.flur4.roomiefunds.models.hbci.SaveHbciConfigDto;
import io.quarkus.cache.CacheInvalidateAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

import java.util.List;
import java.util.NoSuchElementException;

@Path("/api/hbci")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class HbciController {
    final GetHbciConfig getHbciConfig;
    final SaveHbciConfig saveHbciConfig;
    final SyncBankTransactions syncBankTransactions;

    @GET
    @Path("/configs")
    public List<HbciConfig> getConfigs() {
        return getHbciConfig.getConfigs();
    }

    @POST
    @Path("/configs")
    public Response createConfig(@Valid SaveHbciConfigDto dto) {
        return Response.status(Response.Status.CREATED).entity(saveHbciConfig.createConfig(dto)).build();
    }

    @GET
    @Path("/configs/{accountId:\\d+}")
    public HbciConfig getConfig(@PathParam("accountId") long accountId) {
        return getHbciConfig.getConfigByAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("No HBCI config for account " + accountId));
    }

    @PUT
    @Path("/configs/{accountId:\\d+}")
    public void updateConfig(@PathParam("accountId") long accountId, @Valid SaveHbciConfigDto dto) {
        try {
            saveHbciConfig.updateConfig(accountId, dto);
        } catch (NoSuchElementException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    @DELETE
    @Path("/configs/{accountId:\\d+}")
    public void deleteConfig(@PathParam("accountId") long accountId) {
        try {
            saveHbciConfig.deleteConfig(accountId);
        } catch (NoSuchElementException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    @POST
    @Path("/configs/{accountId:\\d+}/sync")
    @CacheInvalidateAll(cacheName = "accounts-with-balances")
    public HbciSyncResult sync(@PathParam("accountId") long accountId) {
        try {
            return syncBankTransactions.sync(accountId);
        } catch (HbciSyncException e) {
            log.errorf("HBCI sync failed for account %d: %s", accountId, e.getMessage());
            throw new InternalServerErrorException(e.getMessage(), e);
        } catch (Exception e) {
            log.errorf(e, "Unexpected error during HBCI sync for account %d", accountId);
            throw new InternalServerErrorException("Sync failed unexpectedly", e);
        }
    }

    @GET
    @Path("/ibans")
    public List<AccountIban> getIbans() {
        return getHbciConfig.getIbans();
    }

    @POST
    @Path("/ibans")
    public Response addIban(@Valid CreateAccountIbanDto dto) {
        return Response.status(Response.Status.CREATED).entity(saveHbciConfig.addIban(dto)).build();
    }

    @DELETE
    @Path("/ibans/{id:\\d+}")
    public void deleteIban(@PathParam("id") long id) {
        try {
            saveHbciConfig.deleteIban(id);
        } catch (NoSuchElementException e) {
            throw new NotFoundException(e.getMessage());
        }
    }
}
