package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.account.*;
import de.flur4.roomiefunds.infrastructure.Utils;
import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.account.CreateAccountDto;
import de.flur4.roomiefunds.models.account.UpdateAccountDto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.jwt.JsonWebToken;

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
    final JsonWebToken jwt;

    @GET
    public List<Account> getAccounts() {
        return getAccount.getAccounts();
    }

    @GET
    @Path("/{accountId:\\d+}")
    public Account getAccount(@PathParam("accountId") long accountId) {
        var result = getAccount.getAccount(accountId);
        if(result.isEmpty()) {
            throw new NotFoundException("Account with id " + accountId + " not found");
        }
        return result.get();
    }

    @POST
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
}
