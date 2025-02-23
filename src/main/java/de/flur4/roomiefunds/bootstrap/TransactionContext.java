package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.transaction.CreateTransaction;
import de.flur4.roomiefunds.domain.api.transaction.DeleteTransaction;
import de.flur4.roomiefunds.domain.api.transaction.GetTransaction;
import de.flur4.roomiefunds.domain.api.transaction.UpdateTransaction;
import de.flur4.roomiefunds.domain.api.transaction.impl.TransactionService;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class TransactionContext {
    @Produces
    @ApplicationScoped
    public GetTransaction getTransaction(TransactionRepository transactionRepository, LogRepository logRepository) {
        return new TransactionService(transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public CreateTransaction createTransaction(TransactionRepository transactionRepository, LogRepository logRepository) {
        return new TransactionService(transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public UpdateTransaction updateTransaction(TransactionRepository transactionRepository, LogRepository logRepository) {
        return new TransactionService(transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public DeleteTransaction deleteTransaction(TransactionRepository transactionRepository, LogRepository logRepository) {
        return new TransactionService(transactionRepository, logRepository);
    }
}
