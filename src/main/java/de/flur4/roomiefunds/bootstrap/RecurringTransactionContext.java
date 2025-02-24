package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.recurringtransaction.CreateRecurringTransaction;
import de.flur4.roomiefunds.domain.api.recurringtransaction.DeleteRecurringTransaction;
import de.flur4.roomiefunds.domain.api.recurringtransaction.GetRecurringTransaction;
import de.flur4.roomiefunds.domain.api.recurringtransaction.UpdateRecurringTransaction;
import de.flur4.roomiefunds.domain.api.recurringtransaction.impl.RecurringTransactionService;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.RecurringTransactionRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class RecurringTransactionContext {
    @Produces
    @ApplicationScoped
    public GetRecurringTransaction getRecurringTransaction(RecurringTransactionRepository recurringTransactionRepository, TransactionRepository transactionRepository, LogRepository logRepository) {
        return new RecurringTransactionService(recurringTransactionRepository, transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public CreateRecurringTransaction createRecurringTransaction(RecurringTransactionRepository recurringTransactionRepository, TransactionRepository transactionRepository, LogRepository logRepository) {
        return new RecurringTransactionService(recurringTransactionRepository, transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public UpdateRecurringTransaction updateRecurringTransaction(RecurringTransactionRepository recurringTransactionRepository, TransactionRepository transactionRepository, LogRepository logRepository) {
        return new RecurringTransactionService(recurringTransactionRepository, transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public DeleteRecurringTransaction deleteRecurringTransaction(RecurringTransactionRepository recurringTransactionRepository, TransactionRepository transactionRepository, LogRepository logRepository) {
        return new RecurringTransactionService(recurringTransactionRepository, transactionRepository, logRepository);
    }
}
