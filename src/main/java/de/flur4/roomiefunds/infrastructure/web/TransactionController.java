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
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.multipart.FileUpload;

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
    @Path("/account/{accountId:\\d+}")
    public List<TransactionSaldoDto> getTransactionForAccount(@PathParam("accountId") long accountId) {
        return getTransaction.getTransactionsForAccount(accountId);
    }

    @GET
    @Path("/{transactionId:\\d+}")
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
    @Path("/{transactionId:\\d+}")
    public Transaction updateTransaction(@PathParam("transactionId") long transactionId, @Valid UpdateTransactionDto updateTransactionDto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return updateTransaction.updateTransaction(modifyingPerson, transactionId, updateTransactionDto);
        } catch (TransactionNotFoundException e) {
            throw new NotFoundException("Transaction with id " + transactionId + "not found");
        } catch (Exception e) {
            log.error("An error occurred while updating transaction", e);
            throw new InternalServerErrorException("An error occurred while updating transaction", e);
        }
    }

    @DELETE
    @Path("/{transactionId:\\d+}")
    public void deleteTransaction(@PathParam("transactionId") long transactionId) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            deleteTransaction.deleteTransaction(modifyingPerson, transactionId);
        } catch (TransactionNotFoundException e) {
            throw new NotFoundException("Transaction with id " + transactionId + "not found");
        } catch (Exception e) {
            log.error("An error occurred while deleting transaction", e);
            throw new InternalServerErrorException("An error occurred while deleting transaction", e);
        }
    }

    @GET
    @Path("/{transactionId:\\d+}/receipt")
    public RestResponse<byte[]> getReceipt(@PathParam("transactionId") long transactionId) {
        var fetchResult = getTransaction.getTransactionReceipt(transactionId);
        if (fetchResult.isEmpty()) {
            throw new NotFoundException("There is no receipt for transaction %d".formatted(transactionId));
        }
        var receipt = fetchResult.get();
        return RestResponse.ResponseBuilder
                .ok(receipt.receipt())
                .header("Content-Type", receipt.receiptMimeType())
                .build();
    }

    @POST
    @Path("/{transactionId:\\d+}/receipt")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void storeReceipt(@PathParam("transactionId") long transactionId,
                             @RestForm("receipt") FileUpload fileUpload) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            updateTransaction.setTransactionReceipt(modifyingPerson, transactionId, fileUpload);
        } catch (TransactionNotFoundException e) {
            throw new NotFoundException("Transaction with id " + transactionId + "not found");
        } catch (Exception e) {
            log.error("An error occurred while deleting transaction receipt", e);
            throw new InternalServerErrorException("An error occurred while deleting transaction receipt", e);
        }
    }

    @DELETE
    @Path("/{transactionId:\\d+}/receipt")
    public void deleteReceipt(@PathParam("transactionId") long transactionId) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            deleteTransaction.deleteTransactionReceipt(modifyingPerson, transactionId);
        } catch (TransactionNotFoundException e) {
            throw new NotFoundException("Transaction with id " + transactionId + "not found");
        } catch (Exception e) {
            log.error("An error occurred while deleting transaction receipt", e);
            throw new InternalServerErrorException("An error occurred while deleting transaction receipt", e);
        }

    }
}
