package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.AccountIbanRepository;
import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.ACCOUNT_IBAN;
import static org.jooq.Records.mapping;

@ApplicationScoped
@RequiredArgsConstructor
public class AccountIbanRepositoryImpl implements AccountIbanRepository {
    final DSLContext jooq;

    @Override
    public List<AccountIban> findAll() {
        return jooq.select(ACCOUNT_IBAN.ID, ACCOUNT_IBAN.ACCOUNT_ID, ACCOUNT_IBAN.IBAN)
                .from(ACCOUNT_IBAN)
                .orderBy(ACCOUNT_IBAN.ID)
                .fetch(mapping(AccountIban::new));
    }

    @Override
    public Optional<Long> findAccountByIban(String iban) {
        return jooq.select(ACCOUNT_IBAN.ACCOUNT_ID)
                .from(ACCOUNT_IBAN)
                .where(ACCOUNT_IBAN.IBAN.eq(iban))
                .fetchOptional(r -> r.value1());
    }

    @Override
    public AccountIban save(CreateAccountIbanDto dto) {
        long newId = jooq.insertInto(ACCOUNT_IBAN)
                .columns(ACCOUNT_IBAN.ACCOUNT_ID, ACCOUNT_IBAN.IBAN)
                .values(dto.accountId(), dto.iban())
                .returningResult(ACCOUNT_IBAN.ID)
                .fetchOne().value1();
        return jooq.select(ACCOUNT_IBAN.ID, ACCOUNT_IBAN.ACCOUNT_ID, ACCOUNT_IBAN.IBAN)
                .from(ACCOUNT_IBAN)
                .where(ACCOUNT_IBAN.ID.eq(newId))
                .fetchOne(mapping(AccountIban::new));
    }

    @Override
    public void deleteById(long id) {
        jooq.deleteFrom(ACCOUNT_IBAN).where(ACCOUNT_IBAN.ID.eq(id)).execute();
    }
}
