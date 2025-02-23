package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.transaction.*;
import de.flur4.roomiefunds.infrastructure.Utils;
import de.flur4.roomiefunds.models.transaction.CreateTransactionDto;
import de.flur4.roomiefunds.models.transaction.Transaction;
import de.flur4.roomiefunds.models.transaction.TransactionSaldoDto;
import de.flur4.roomiefunds.models.transaction.UpdateTransactionDto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/api/transaction")
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class TransactionController {
    final GetTransaction getTransaction;
    final CreateTransaction createTransaction;
    final UpdateTransaction updateTransaction;
    final DeleteTransaction deleteTransaction;
    final JsonWebToken jwt;

    @GET
    @Path("/account/{accountId}:\\d+")
    public List<TransactionSaldoDto> getTransactionForAccount(@PathParam("accountId") long accountId) {
        return getTransaction.getTransactionsForAccount(accountId);
    }

    @GET
    @Path("/{transactionId}:\\d+")
    public Transaction getTransaction(@PathParam("transactionId") long transactionId) {
        var result = getTransaction.getTransaction(transactionId);
        if (result.isEmpty()) {
            throw new NotFoundException("Transaction with id " + transactionId + "not found");
        }
        return result.get();
    }

    @POST
    public Transaction createTransaction(@Valid CreateTransactionDto createTransactionDto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return createTransaction.createTransaction(modifyingPerson, createTransactionDto);
        } catch (Exception e) {
            log.error("An error occurred while creating transaction", e);
            throw new InternalServerErrorException("An error occurred while creating transaction", e);
        }
    }

    @PATCH
    @Path("/{transactionId}:\\d+")
    public Transaction updateTransaction(@PathParam("transactionId") long transactionId, @Valid UpdateTransactionDto updateTransactionDto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return updateTransaction.updateTransaction(modifyingPerson, transactionId, updateTransactionDto);
        } catch (TransactionNotFoundException e) {
            throw new NotFoundException("Transaction with id " + transactionId + "not found");
        } catch (Exception e) {
            log.error("An error occurred while creating transaction", e);
            throw new InternalServerErrorException("An error occurred while creating transaction", e);
        }
    }

    @DELETE
    @Path("/{transactionId}:\\d+")
    public void deleteTransaction(@PathParam("transactionId") long transactionId) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            deleteTransaction.deleteTransaction(modifyingPerson, transactionId);
        } catch (TransactionNotFoundException e) {
            throw new NotFoundException("Transaction with id " + transactionId + "not found");
        } catch (Exception e) {
            log.error("An error occurred while creating transaction", e);
            throw new InternalServerErrorException("An error occurred while creating transaction", e);
        }
    }
}
