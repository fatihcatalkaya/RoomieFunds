package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.account.CreateAccountDto;
import de.flur4.roomiefunds.models.account.UpdateAccountDto;
import org.jooq.exception.DataAccessException;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> getAccount(long accountId);

    List<Account> getAllAccounts();

    Account createAccount(CreateAccountDto createAccountDto);

    Account updateAccount(long accountId, UpdateAccountDto updateAccountDto);

    void deleteAccount(long accountId) throws DataAccessException;
}
