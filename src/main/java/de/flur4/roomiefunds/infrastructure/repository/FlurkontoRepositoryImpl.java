package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.AccountRepository;
import de.flur4.roomiefunds.domain.spi.FlurkontoRepository;
import de.flur4.roomiefunds.models.account.Account;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.Optional;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.SETTINGS;

@ApplicationScoped
@RequiredArgsConstructor
public class FlurkontoRepositoryImpl implements FlurkontoRepository {
    final static String SETTINGS_FLURKONTO_ID = "flur_account_id";
    final DSLContext jooq;
    final AccountRepository accountRepository;

    @Override
    public Optional<Account> getFlurkonto() {
        var flurkontoId = jooq.select(SETTINGS.VALUE_INT)
                .from(SETTINGS)
                .where(SETTINGS.SETTING_KEY.eq(SETTINGS_FLURKONTO_ID))
                .fetchOne().value1();
        return accountRepository.getAccount(flurkontoId);
    }

    @Override
    public Account setFlurkonto(long accountId) {
        jooq.update(SETTINGS)
                .set(SETTINGS.VALUE_INT, accountId)
                .where(SETTINGS.SETTING_KEY.eq(SETTINGS_FLURKONTO_ID))
                .execute();
        return accountRepository.getAccount(accountId).get();
    }
}
