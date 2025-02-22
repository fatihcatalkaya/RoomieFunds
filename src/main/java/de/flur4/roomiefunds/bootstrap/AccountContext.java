package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.account.CreateAccount;
import de.flur4.roomiefunds.domain.api.account.DeleteAccount;
import de.flur4.roomiefunds.domain.api.account.GetAccount;
import de.flur4.roomiefunds.domain.api.account.UpdateAccount;
import de.flur4.roomiefunds.domain.api.account.impl.AccountService;
import de.flur4.roomiefunds.domain.spi.AccountRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class AccountContext {
    @Produces
    @ApplicationScoped
    public CreateAccount createAccount(AccountRepository accountRepository, TransactionRepository transactionRepository, LogRepository logRepository) {
        return new AccountService(accountRepository, transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public UpdateAccount updateAccount(AccountRepository accountRepository, TransactionRepository transactionRepository, LogRepository logRepository) {
        return new AccountService(accountRepository, transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public GetAccount getAccount(AccountRepository accountRepository, TransactionRepository transactionRepository, LogRepository logRepository) {
        return new AccountService(accountRepository, transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public DeleteAccount deleteAccount(AccountRepository accountRepository, TransactionRepository transactionRepository, LogRepository logRepository) {
        return new AccountService(accountRepository, transactionRepository, logRepository);
    }
}
