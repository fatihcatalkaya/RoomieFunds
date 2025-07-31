package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.account.*;
import de.flur4.roomiefunds.infrastructure.Utils;
import de.flur4.roomiefunds.infrastructure.cache.AccountsWithBalancesCacheKeyGenerator;
import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.account.AccountWithBalance;
import de.flur4.roomiefunds.models.account.CreateAccountDto;
import de.flur4.roomiefunds.models.account.UpdateAccountDto;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.vertx.core.eventbus.EventBus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.List;

@Path("/api/account")
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class AccountController {
    final GetAccount getAccount;
    final CreateAccount createAccount;
    final UpdateAccount updateAccount;
    final DeleteAccount deleteAccount;
    final PrintAccountStatement printAccountStatement;
    final JsonWebToken jwt;
    @Inject
    EventBus eventBus;

    @GET
    public List<Account> getAccounts() {
        return getAccount.getAccounts();
    }

    @GET
    @Path("/with-balances")
    @CacheResult(cacheName = "accounts-with-balances", keyGenerator = AccountsWithBalancesCacheKeyGenerator.class)
    public List<AccountWithBalance> getAccountsWithBalances() {
        return getAccount.getAccountsWithBalances();
    }

    @GET
    @Path("/{accountId:\\d+}")
    public Account getAccount(@PathParam("accountId") long accountId) {
        var result = getAccount.getAccount(accountId);
        if (result.isEmpty()) {
            throw new NotFoundException("Account with id " + accountId + " not found");
        }
        return result.get();
    }

    @GET
    @Path("/{accountId:\\d+}/statement")
    @Produces(value = "application/pdf")
    public byte[] getAccountStatement(@PathParam("accountId") long accountId) {
        try {
            return printAccountStatement.printAccountStatement(accountId);
        } catch (AccountNotFoundException ignored) {
            throw new NotFoundException("Account with id " + accountId + " not found");
        } catch (Exception ex) {
            log.error("An error occurred while rendering account statement for account id " + accountId, ex);
            throw new InternalServerErrorException("An error occurred while creating account", ex);
        }
    }

    @POST
    @CacheInvalidateAll(cacheName = "accounts-with-balances")
    public Account createAccount(@Valid CreateAccountDto dto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return createAccount.createAccount(modifyingPerson, dto);
        } catch (Exception e) {
            log.error("An error occurred while creating account", e);
            throw new InternalServerErrorException("An error occurred while creating account", e);
        }
    }

    @PATCH
    @Path("/{accountId:\\d+}")
    @CacheInvalidateAll(cacheName = "accounts-with-balances")
    public Account patchAccount(@PathParam("accountId") long accountId, @Valid UpdateAccountDto dto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return updateAccount.updateAccount(modifyingPerson, accountId, dto);
        } catch (AccountNotFoundException e) {
            throw new NotFoundException("Account with id " + accountId + " not found");
        } catch (Exception e) {
            log.error("An error occurred while creating account", e);
            throw new InternalServerErrorException("An error occurred while updating account", e);
        }
    }

    @DELETE
    @Path("/{accountId:\\d+}")
    @CacheInvalidateAll(cacheName = "accounts-with-balances")
    public void deleteAccount(@PathParam("accountId") long accountId) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            deleteAccount.deleteAccount(modifyingPerson, accountId);
        } catch (AccountNotFoundException e) {
            throw new NotFoundException("Account with id " + accountId + " not found");
        } catch (AccountUndeletableException e) {
            throw new ClientErrorException(e.getMessage(), Response.Status.CONFLICT);
        } catch (Exception e) {
            log.error("An error occurred while creating account", e);
            throw new InternalServerErrorException("An error occurred while deleting account", e);
        }
    }

    @GET
    @Path("/send-account-statements-now")
    public RestResponse<?> sendAccountStatementsNow() {
        eventBus.send("send-account-statements-emails", null);
        return RestResponse.noContent();
    }
}
