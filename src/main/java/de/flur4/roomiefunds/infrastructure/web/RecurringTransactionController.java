package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.recurringtransaction.*;
import de.flur4.roomiefunds.infrastructure.Utils;
import de.flur4.roomiefunds.models.recurringtransaction.CreateRecurringTransactionDto;
import de.flur4.roomiefunds.models.recurringtransaction.GetRecurringTransactionDto;
import de.flur4.roomiefunds.models.recurringtransaction.UpdateRecurringTransactionDto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/api/recurring-transaction")
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class RecurringTransactionController {
    final GetRecurringTransaction getRecurringTransaction;
    final CreateRecurringTransaction createRecurringTransaction;
    final UpdateRecurringTransaction updateRecurringTransaction;
    final DeleteRecurringTransaction deleteRecurringTransaction;
    final JsonWebToken jwt;

    @GET
    public List<GetRecurringTransactionDto> getRecurringTransactions() {
        return getRecurringTransaction.getRecurringTransactions();
    }

    @GET
    @Path("/{recurringTransactionId:\\d+}")
    public GetRecurringTransactionDto getRecurringTransaction(@PathParam("recurringTransactionId") long recurringTransactionId) {
        var result = getRecurringTransaction.getRecurringTransaction(recurringTransactionId);
        if (result.isEmpty()) {
            throw new NotFoundException("Recurring Transaction with id " + recurringTransactionId + "not found");
        }
        return result.get();
    }

    @POST
    public GetRecurringTransactionDto createRecurringTransaction(@Valid CreateRecurringTransactionDto dto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return createRecurringTransaction.createRecurringTransaction(modifyingPerson, dto);
        } catch (Exception e) {
            log.error("An error occurred while creating recurring transaction", e);
            throw new InternalServerErrorException("An error occurred while creating recurring transaction", e);
        }
    }

    @PATCH
    @Path("/{recurringTransactionId:\\d+}")
    public GetRecurringTransactionDto updateRecurringTransaction(@PathParam("recurringTransactionId") long recurringTransactionId, @Valid UpdateRecurringTransactionDto dto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return updateRecurringTransaction.updateRecurringTransaction(modifyingPerson, recurringTransactionId, dto);
        } catch (RecurringTransactionNotFoundException e) {
            throw new NotFoundException("Recurring Transaction with id " + recurringTransactionId + "not found");
        } catch (Exception e) {
            log.error("An error occurred while updating recurring transaction", e);
            throw new InternalServerErrorException("An error occurred while updating recurring transaction", e);
        }
    }

    @DELETE
    @Path("/{recurringTransactionId:\\d+}")
    public void deleteRecurringTransaction(@PathParam("recurringTransactionId") long recurringTransactionId) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            deleteRecurringTransaction.deleteRecurringTransaction(modifyingPerson, recurringTransactionId);
        } catch (RecurringTransactionNotFoundException e) {
            throw new NotFoundException("Recurring Transaction with id " + recurringTransactionId + "not found");
        } catch (Exception e) {
            log.error("An error occurred while deleting recurring transaction", e);
            throw new InternalServerErrorException("An error occurred while deleting recurring transaction", e);
        }
    }
}
