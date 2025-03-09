package de.flur4.roomiefunds.domain.api.account.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.account.*;
import de.flur4.roomiefunds.domain.spi.AccountRepository;
import de.flur4.roomiefunds.domain.spi.AccountStatementRenderer;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.account.CreateAccountDto;
import de.flur4.roomiefunds.models.account.UpdateAccountDto;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class AccountService implements CreateAccount, GetAccount, UpdateAccount, DeleteAccount, PrintAccountStatement {
    final AccountRepository accountRepository;
    final TransactionRepository transactionRepository;
    final AccountStatementRenderer accountStatementRenderer;
    final LogRepository logRepository;

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
        if(account.isEmpty()){
            throw new AccountNotFoundException(accountId);
        }
        var transactions = transactionRepository.getTransactionsByAccountId(accountId);
        return accountStatementRenderer.renderAccountStatement(account.get(), transactions);
    }
}
