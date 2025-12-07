package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.account.*;
import de.flur4.roomiefunds.domain.api.account.impl.AccountService;
import de.flur4.roomiefunds.domain.api.transaction.GetTransaction;
import de.flur4.roomiefunds.domain.spi.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class AccountContext {
    @Produces
    @ApplicationScoped
    public CreateAccount createAccount(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountStatementRenderer accountStatementRenderer,
            AccountStatementMailer accountStatementMailer,
            PersonRepository personRepository,
            LogRepository logRepository,
            GetTransaction getTransaction,
            AccountReportRenderer accountReportRenderer) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, accountStatementMailer, personRepository, logRepository, getTransaction, accountReportRenderer);
    }

    @Produces
    @ApplicationScoped
    public UpdateAccount updateAccount(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountStatementRenderer accountStatementRenderer,
            AccountStatementMailer accountStatementMailer,
            PersonRepository personRepository,
            LogRepository logRepository,
            GetTransaction getTransaction,
            AccountReportRenderer accountReportRenderer) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, accountStatementMailer, personRepository, logRepository, getTransaction, accountReportRenderer);
    }

    @Produces
    @ApplicationScoped
    public GetAccount getAccount(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountStatementRenderer accountStatementRenderer,
            AccountStatementMailer accountStatementMailer,
            PersonRepository personRepository,
            LogRepository logRepository,
            GetTransaction getTransaction,
            AccountReportRenderer accountReportRenderer) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, accountStatementMailer, personRepository, logRepository, getTransaction, accountReportRenderer);
    }

    @Produces
    @ApplicationScoped
    public DeleteAccount deleteAccount(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountStatementRenderer accountStatementRenderer,
            AccountStatementMailer accountStatementMailer,
            PersonRepository personRepository,
            LogRepository logRepository,
            GetTransaction getTransaction,
            AccountReportRenderer accountReportRenderer) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, accountStatementMailer, personRepository, logRepository, getTransaction, accountReportRenderer);
    }

    @Produces
    @ApplicationScoped
    public PrintAccountStatement printAccountStatement(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountStatementRenderer accountStatementRenderer,
            AccountStatementMailer accountStatementMailer,
            PersonRepository personRepository,
            LogRepository logRepository,
            GetTransaction getTransaction,
            AccountReportRenderer accountReportRenderer) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, accountStatementMailer, personRepository, logRepository, getTransaction, accountReportRenderer);
    }

    @Produces
    @ApplicationScoped
    public SendAccountStatements sendAccountStatements(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountStatementRenderer accountStatementRenderer,
            AccountStatementMailer accountStatementMailer,
            PersonRepository personRepository,
            LogRepository logRepository,
            GetTransaction getTransaction,
            AccountReportRenderer accountReportRenderer) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, accountStatementMailer, personRepository, logRepository, getTransaction, accountReportRenderer);
    }

    @Produces
    @ApplicationScoped
    public GetAccountReport getAccountReport(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            AccountStatementRenderer accountStatementRenderer,
            AccountStatementMailer accountStatementMailer,
            PersonRepository personRepository,
            LogRepository logRepository,
            GetTransaction getTransaction,
            AccountReportRenderer accountReportRenderer) {
        return new AccountService(accountRepository, transactionRepository, accountStatementRenderer, accountStatementMailer, personRepository, logRepository, getTransaction, accountReportRenderer);
    }
}
