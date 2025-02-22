package de.flur4.roomiefunds.domain.api.account;

public class AccountUndeletableException extends Exception {
    public AccountUndeletableException(String accountName, long accountId) {
        super("Account '%s' (%d) can not be deleted, because there are already associated transactions.".formatted(accountName, accountId));
    }
}
