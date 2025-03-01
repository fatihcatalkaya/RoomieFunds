package de.flur4.roomiefunds.domain.api.transaction.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.transaction.*;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import de.flur4.roomiefunds.models.transaction.*;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class TransactionService implements GetTransaction, CreateTransaction, UpdateTransaction, DeleteTransaction {
    final TransactionRepository transactionRepository;
    final LogRepository logRepository;

    @Override
    public Optional<Transaction> getTransaction(long transactionId) {
        return transactionRepository.getTransactionById(transactionId);
    }

    @Override
    public Optional<ReceiptDto> getTransactionReceipt(long transactionId) {
        return transactionRepository.getTransactionReceipt(transactionId);
    }

    @Override
    public List<TransactionSaldoDto> getTransactionsForAccount(long accountId) {
        var transactions = transactionRepository.getTransactionsByAccountId(accountId);
        List<TransactionSaldoDto> transactionSaldoDtos = new ArrayList<>(transactions.size());
        long saldo = 0;
        for (int i = 0; i < transactions.size(); i++) {
            final Transaction t = transactions.get(i);
            if (t.sourceAccountActive() != t.targetAccountActive()) {
                saldo += t.amount();
            } else if (t.sourceAccountId() == accountId) {
                saldo -= t.amount();
            } else {
                saldo += t.amount();
            }

            transactionSaldoDtos.add(new TransactionSaldoDto(
                    transactions.get(i),
                    saldo,
                    t.sourceAccountName().split(":"),
                    t.targetAccountName().split(":")
            ));
        }

        return transactionSaldoDtos;
    }

    @Override
    public Transaction createTransaction(ModifyingPersonDto modifyingPerson, CreateTransactionDto createTransactionDto) throws JsonProcessingException {
        var result = transactionRepository.createTransaction(createTransactionDto);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.create,
                "transaction",
                Optional.empty(),
                Optional.of(result)
        ));
        return result;
    }

    @Override
    public Transaction updateTransaction(ModifyingPersonDto modifyingPerson, long transactionId, UpdateTransactionDto updateTransactionDto) throws TransactionNotFoundException, JsonProcessingException, IllegalArgumentException {
        var fetchResult = transactionRepository.getTransactionById(transactionId);
        if(fetchResult.isEmpty()) {
            throw new TransactionNotFoundException(transactionId);
        }
        var transactionBefore = fetchResult.get();
        var transactionAfter = transactionRepository.updateTransaction(transactionId, updateTransactionDto);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.update,
                "transaction",
                Optional.of(transactionBefore),
                Optional.of(transactionAfter)
        ));
        return transactionAfter;
    }

    @Override
    public void setTransactionReceipt(ModifyingPersonDto modifyingPerson, long transactionId, FileUpload fileUpload) throws TransactionNotFoundException, IOException {
        var transaction = transactionRepository.getTransactionById(transactionId);
        if(transaction.isEmpty()) {
            throw new TransactionNotFoundException(transactionId);
        }
        var transactionBefore = transaction.get();
        var transactionAfter = transactionRepository.setTransactionReceipt(transactionId, fileUpload);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.update,
                "transaction",
                Optional.of(transactionBefore),
                Optional.of(transactionAfter)
        ));
    }

    @Override
    public void deleteTransaction(ModifyingPersonDto modifyingPerson, long transactionId) throws TransactionNotFoundException, JsonProcessingException {
        var transaction = transactionRepository.getTransactionById(transactionId);
        if(transaction.isEmpty()) {
            throw new TransactionNotFoundException(transactionId);
        }
        var transactionBefore = transaction.get();
        transactionRepository.deleteTransaction(transactionId);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.delete,
                "transaction",
                Optional.of(transactionBefore),
                Optional.empty()
        ));
    }

    @Override
    public void deleteTransactionReceipt(ModifyingPersonDto modifyingPerson, long transactionId) throws TransactionNotFoundException, JsonProcessingException {
        var transaction = transactionRepository.getTransactionById(transactionId);
        if(transaction.isEmpty()) {
            throw new TransactionNotFoundException(transactionId);
        }
        var transactionBefore = transaction.get();
        var transactionAfter = transactionRepository.deleteTransactionReceipt(transactionId);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.update,
                "transaction",
                Optional.of(transactionBefore),
                Optional.of(transactionAfter)
        ));
    }
}
