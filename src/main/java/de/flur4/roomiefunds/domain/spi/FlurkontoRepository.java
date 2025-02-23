package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.account.Account;

import java.util.Optional;

public interface FlurkontoRepository {
    Optional<Account> getFlurkonto();
    Account setFlurkonto(long accountId);
}
