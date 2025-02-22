package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.AccountRepository;
import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.account.CreateAccountDto;
import de.flur4.roomiefunds.models.account.UpdateAccountDto;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.util.List;
import java.util.Optional;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.ACCOUNT;
import static org.jooq.Records.mapping;

@ApplicationScoped
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {
    final DSLContext jooq;

    @Override
    public Optional<Account> getAccount(long accountId) {
        return jooq.select(ACCOUNT.ID, ACCOUNT.NAME, ACCOUNT.IS_ACTIVE)
                .from(ACCOUNT)
                .where(ACCOUNT.ID.eq(accountId))
                .fetchOptional(mapping(Account::new));
    }

    @Override
    public List<Account> getAllAccounts() {
        return jooq.select(ACCOUNT.ID, ACCOUNT.NAME, ACCOUNT.IS_ACTIVE)
                .from(ACCOUNT)
                .orderBy(ACCOUNT.ID)
                .fetch(mapping(Account::new));
    }

    @Override
    public Account createAccount(CreateAccountDto createAccountDto) {
        return jooq.insertInto(ACCOUNT)
                .columns(ACCOUNT.NAME, ACCOUNT.IS_ACTIVE)
                .values(createAccountDto.name(), createAccountDto.active())
                .returningResult(ACCOUNT.ID, ACCOUNT.NAME, ACCOUNT.IS_ACTIVE)
                .fetchOne(mapping(Account::new));
    }

    @Override
    public Account updateAccount(long accountId, UpdateAccountDto updateDto) {
        var account = jooq.selectFrom(ACCOUNT).where(ACCOUNT.ID.eq(accountId)).fetchOne();
        assert account != null;
        if (updateDto.name().isPresent()) {
            account.setName(updateDto.name().get());
        }
        if (updateDto.active().isPresent()) {
            account.setIsActive(updateDto.active().get());
        }
        account.store();
        return new Account(account.getId(), account.getName(), account.getIsActive());
    }

    @Override
    public void deleteAccount(long accountId) throws DataAccessException {
        jooq.deleteFrom(ACCOUNT).where(ACCOUNT.ID.eq(accountId)).execute();
    }
}
