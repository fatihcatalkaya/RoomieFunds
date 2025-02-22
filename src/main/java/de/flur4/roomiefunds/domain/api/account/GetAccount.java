package de.flur4.roomiefunds.domain.api.account;

import de.flur4.roomiefunds.models.account.Account;

import java.util.List;
import java.util.Optional;

public interface GetAccount {
    Optional<Account> getAccount(long accountId);
    List<Account> getAccounts();
}
