package de.flur4.roomiefunds.domain.api.flurkonto;

import de.flur4.roomiefunds.models.account.Account;

import java.util.Optional;

public interface GetFlurkonto {
    Optional<Account> getFlurkonto();
}
