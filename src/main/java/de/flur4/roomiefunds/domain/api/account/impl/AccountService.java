package de.flur4.roomiefunds.domain.api.account.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.account.*;
import de.flur4.roomiefunds.domain.api.transaction.GetTransaction;
import de.flur4.roomiefunds.domain.spi.*;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.models.account.*;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import de.flur4.roomiefunds.models.person.Person;
import de.flur4.roomiefunds.models.transaction.TransactionSaldoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.javatuples.Pair;

import java.util.*;

@JBossLog
@RequiredArgsConstructor
public class AccountService implements CreateAccount, GetAccount, UpdateAccount, DeleteAccount, PrintAccountStatement, SendAccountStatements {
    final AccountRepository accountRepository;
    final TransactionRepository transactionRepository;
    final AccountStatementRenderer accountStatementRenderer;
    final AccountStatementMailer accountStatementMailer;
    final PersonRepository personRepository;
    final LogRepository logRepository;
    final GetTransaction getTransaction;

    @Override
    public Account createAccount(ModifyingPersonDto modifyingPerson, CreateAccountDto createAccountDto) throws JsonProcessingException {
        var result = accountRepository.createAccount(createAccountDto);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.create,
                "account",
                Optional.empty(),
                Optional.of(result)
        ));
        return result;
    }

    @Override
    public void deleteAccount(ModifyingPersonDto modifyingPerson, long accountId) throws AccountNotFoundException, AccountUndeletableException, JsonProcessingException {
        var fetchResult = accountRepository.getAccount(accountId);
        if (fetchResult.isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        var account = fetchResult.get();
        if (transactionRepository.accountHasTransactions(accountId)) {
            throw new AccountUndeletableException(account.name(), accountId);
        }
        accountRepository.deleteAccount(accountId);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.delete,
                "account",
                Optional.of(account),
                Optional.empty()
        ));
    }

    @Override
    public Optional<Account> getAccount(long accountId) {
        return accountRepository.getAccount(accountId);
    }

    @Override
    public List<Account> getAccounts() {
        return accountRepository.getAllAccounts();
    }

    @Override
    public List<AccountWithBalance> getAccountsWithBalances(boolean includeDisabled) {
        log.info("Getting accounts with balances");
        List<Account> accounts = accountRepository.getAllAccounts();

        List<Person> inactivePeople = personRepository.getAllPersons();
        inactivePeople.removeAll(personRepository.getPersonsThatPayFlurbeitrag());
        inactivePeople.removeAll(personRepository.getPersonsToPrintOnTallyList());

        List<AccountWithBalance> accountsWithBalances = new ArrayList<>(accounts.size());

        for (Account account : accounts) {
            List<TransactionSaldoDto> transactions = getTransaction.getTransactionsForAccount(account.id());

            if (!includeDisabled) {
                boolean isConnectedToInactivePerson = inactivePeople.stream().anyMatch(person -> person.accountId() == account.id());
                if (isConnectedToInactivePerson) {
                    continue;
                }
            }

            double balance = 0;
            if(!transactions.isEmpty()) {
                balance = transactions.getLast().saldo();
            }

            accountsWithBalances.add(new AccountWithBalance(account.id(), account.name(), account.active(), balance));
        }

        return accountsWithBalances;
    }

    @Override
    public Account updateAccount(ModifyingPersonDto modifyingPerson, long accountId, UpdateAccountDto updateAccountDto) throws AccountNotFoundException, JsonProcessingException {
        var fetchResult = accountRepository.getAccount(accountId);
        if (fetchResult.isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        var accountBefore = fetchResult.get();
        var accountAfter = accountRepository.updateAccount(accountId, updateAccountDto);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.update,
                "account",
                Optional.of(accountBefore),
                Optional.of(accountAfter)
        ));
        return accountAfter;
    }

    @Override
    public byte[] printAccountStatement(long accountId) throws AccountNotFoundException {
        var account = accountRepository.getAccount(accountId);
        if (account.isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        var transactions = transactionRepository.getTransactionsByAccountId(accountId);
        return accountStatementRenderer.renderAccountStatement(account.get(), transactions);
    }

    @Override
    public SendAccountStatementsResult sendAccountStatements() {
        var persons = personRepository.getPersonsWithValidEmails();
        List<Person> successfulSendPersons = new ArrayList<>();
        List<Pair<Person, Exception>> failedPersons = new ArrayList<>();
        for (var person : persons) {
            try {
                byte[] accountStatement = printAccountStatement(person.accountId());
                accountStatementMailer.sendAccountStatement(person, accountStatement);
                successfulSendPersons.add(person);
            } catch (Exception ex) {
                failedPersons.add(Pair.with(person, ex));
            }
        }
        return new SendAccountStatementsResult(successfulSendPersons, failedPersons);
    }
}
