package de.flur4.roomiefunds.domain.api.account;

public class AccountNotFoundException extends Exception {
    public AccountNotFoundException(long accountId) {
        super("Could not find person with id %d".formatted(accountId));
    }
}
