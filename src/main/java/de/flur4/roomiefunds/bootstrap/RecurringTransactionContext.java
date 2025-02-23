package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.recurringtransaction.CreateRecurringTransaction;
import de.flur4.roomiefunds.domain.api.recurringtransaction.DeleteRecurringTransaction;
import de.flur4.roomiefunds.domain.api.recurringtransaction.GetRecurringTransaction;
import de.flur4.roomiefunds.domain.api.recurringtransaction.UpdateRecurringTransaction;
import de.flur4.roomiefunds.domain.api.recurringtransaction.impl.RecurringTransactionService;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.RecurringTransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class RecurringTransactionContext {
    @Produces
    @ApplicationScoped
    public GetRecurringTransaction getRecurringTransaction(RecurringTransactionRepository recurringTransactionRepository, LogRepository logRepository) {
        return new RecurringTransactionService(recurringTransactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public CreateRecurringTransaction createRecurringTransaction(RecurringTransactionRepository recurringTransactionRepository, LogRepository logRepository) {
        return new RecurringTransactionService(recurringTransactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public UpdateRecurringTransaction updateRecurringTransaction(RecurringTransactionRepository recurringTransactionRepository, LogRepository logRepository) {
        return new RecurringTransactionService(recurringTransactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public DeleteRecurringTransaction deleteRecurringTransaction(RecurringTransactionRepository recurringTransactionRepository, LogRepository logRepository) {
        return new RecurringTransactionService(recurringTransactionRepository, logRepository);
    }
}
