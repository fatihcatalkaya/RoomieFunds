package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.enablebanking.*;
import de.flur4.roomiefunds.domain.api.enablebanking.impl.EnableBankingService;
import de.flur4.roomiefunds.domain.spi.EnableBankingRepository;
import de.flur4.roomiefunds.domain.spi.EnableBankingTransactionFetcher;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class EnableBankingContext {
    @Produces
    @ApplicationScoped
    public GetSession getSession(EnableBankingRepository enableBankingRepository, LogRepository logRepository, EnableBankingTransactionFetcher transactionFetcher) {
        return new EnableBankingService(enableBankingRepository, logRepository, transactionFetcher);
    }

    @Produces
    @ApplicationScoped
    public FinishSession finishSession(EnableBankingRepository enableBankingRepository, LogRepository logRepository, EnableBankingTransactionFetcher transactionFetcher) {
        return new EnableBankingService(enableBankingRepository, logRepository, transactionFetcher);
    }

    @Produces
    @ApplicationScoped
    public DeleteSession deleteSession(EnableBankingRepository enableBankingRepository, LogRepository logRepository, EnableBankingTransactionFetcher transactionFetcher) {
        return new EnableBankingService(enableBankingRepository, logRepository, transactionFetcher);
    }

    @Produces
    @ApplicationScoped
    public StartAuthorization startAuthorization(EnableBankingRepository enableBankingRepository, LogRepository logRepository, EnableBankingTransactionFetcher transactionFetcher) {
        return new EnableBankingService(enableBankingRepository, logRepository, transactionFetcher);
    }

    @Produces
    @ApplicationScoped
    public GetBankTransactions getBankTransactions(EnableBankingRepository enableBankingRepository, LogRepository logRepository, EnableBankingTransactionFetcher transactionFetcher) {
        return new EnableBankingService(enableBankingRepository, logRepository, transactionFetcher);
    }
}
