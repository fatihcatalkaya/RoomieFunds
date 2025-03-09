package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.account.*;
import de.flur4.roomiefunds.domain.api.account.impl.AccountService;
import de.flur4.roomiefunds.domain.spi.AccountRepository;
import de.flur4.roomiefunds.domain.spi.AccountStatementRenderer;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class AccountContext {
    @Produces
    @ApplicationScoped
    public CreateAccount createAccount(AccountRepository accountRepository, TransactionRepository transactionRepository, AccountStatementRenderer accountStatementRenderer, LogRepository logRepository) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, logRepository);
    }

    @Produces
    @ApplicationScoped
    public UpdateAccount updateAccount(AccountRepository accountRepository, TransactionRepository transactionRepository, AccountStatementRenderer accountStatementRenderer, LogRepository logRepository) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, logRepository);
    }

    @Produces
    @ApplicationScoped
    public GetAccount getAccount(AccountRepository accountRepository, TransactionRepository transactionRepository, AccountStatementRenderer accountStatementRenderer, LogRepository logRepository) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, logRepository);
    }

    @Produces
    @ApplicationScoped
    public DeleteAccount deleteAccount(AccountRepository accountRepository, TransactionRepository transactionRepository, AccountStatementRenderer accountStatementRenderer, LogRepository logRepository) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, logRepository);
    }

    @Produces
    @ApplicationScoped
    public PrintAccountStatement printAccountStatement(AccountRepository accountRepository, TransactionRepository transactionRepository, AccountStatementRenderer accountStatementRenderer, LogRepository logRepository) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, logRepository);
    }
}
