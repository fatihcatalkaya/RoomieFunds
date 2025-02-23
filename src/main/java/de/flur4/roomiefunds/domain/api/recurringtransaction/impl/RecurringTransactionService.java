package de.flur4.roomiefunds.domain.api.recurringtransaction.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.recurringtransaction.*;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.RecurringTransactionRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import de.flur4.roomiefunds.models.recurringtransaction.CreateRecurringTransactionDto;
import de.flur4.roomiefunds.models.recurringtransaction.GetRecurringTransactionDto;
import de.flur4.roomiefunds.models.recurringtransaction.UpdateRecurringTransactionDto;
import de.flur4.roomiefunds.models.transaction.CreateTransactionDto;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class RecurringTransactionService implements CreateRecurringTransaction, GetRecurringTransaction, UpdateRecurringTransaction, DeleteRecurringTransaction {

    final static ModifyingPersonDto SCHEDULER = new ModifyingPersonDto("", Optional.of("Recurring Transaction Scheduler"));
    final RecurringTransactionRepository recurringTransactionRepository;
    final TransactionRepository transactionRepository;
    final LogRepository logRepository;

    @Override
    public List<GetRecurringTransactionDto> getRecurringTransactions() {
        return recurringTransactionRepository.getAllRecurringTransactions();
    }

    @Override
    public Optional<GetRecurringTransactionDto> getRecurringTransaction(long recurringTransactionId) {
        return recurringTransactionRepository.getRecurringTransactionById(recurringTransactionId);
    }

    @Override
    public GetRecurringTransactionDto createRecurringTransaction(ModifyingPersonDto modifyingPerson, CreateRecurringTransactionDto createRecurringTransaction) throws JsonProcessingException {
        var recurringTransaction = recurringTransactionRepository.createRecurringTransaction(createRecurringTransaction);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.create,
                "recurring_transaction",
                Optional.empty(),
                Optional.of(recurringTransaction)
        ));
        return recurringTransaction;
    }

    @Override
    public GetRecurringTransactionDto updateRecurringTransaction(ModifyingPersonDto modifyingPerson, long transactionId, UpdateRecurringTransactionDto updateRecurringTransaction) throws JsonProcessingException, RecurringTransactionNotFoundException {
        Optional<GetRecurringTransactionDto> fetchResult = recurringTransactionRepository.getRecurringTransactionById(transactionId);
        if (fetchResult.isEmpty()) {
            throw new RecurringTransactionNotFoundException(transactionId);
        }
        var recurringTransaction = recurringTransactionRepository.updateRecurringTransaction(transactionId, updateRecurringTransaction);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.update,
                "recurring_transaction",
                Optional.of(fetchResult.get()),
                Optional.of(recurringTransaction)
        ));
        return recurringTransaction;
    }

    @Override
    public void deleteRecurringTransaction(ModifyingPersonDto modifyingPerson, long transactionId) throws JsonProcessingException, RecurringTransactionNotFoundException {
        Optional<GetRecurringTransactionDto> recurringTransaction = recurringTransactionRepository.getRecurringTransactionById(transactionId);
        if (recurringTransaction.isEmpty()) {
            throw new RecurringTransactionNotFoundException(transactionId);
        }

        recurringTransactionRepository.deleteRecurringTransaction(transactionId);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.delete,
                "recurring_transaction",
                Optional.of(recurringTransaction.get()),
                Optional.empty()
        ));
    }

    @Override
    public void createScheduledTransactions() {
        final LocalDate today = LocalDate.now(ZoneId.systemDefault());
        final int dayOfMonth = today.getDayOfMonth();
        recurringTransactionRepository.getAllRecurringTransactions()
                .stream()
                .filter(tx -> tx.valueDayOfMonth() == dayOfMonth)
                .filter(tx -> !recurringTransactionRepository.hasTransactionBeenCreatedAlready(tx.id(), today))
                .forEach(tx -> {
                    var createdTx = transactionRepository.createTransaction(new CreateTransactionDto(
                            tx.sourceAccountId(),
                            tx.targetAccountId(),
                            tx.amount(),
                            today,
                            tx.transactionDescription()
                    ));
                    try {
                        logRepository.insertLogEntry(SCHEDULER, new InsertLogEntryDto(
                                LogOperations.create,
                                "transaction",
                                Optional.empty(),
                                Optional.of(createdTx)
                        ));
                    } catch (JsonProcessingException ignored) { }
                });
    }
}
