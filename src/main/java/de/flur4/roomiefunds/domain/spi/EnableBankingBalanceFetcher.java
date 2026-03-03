package de.flur4.roomiefunds.domain.spi;

import java.util.Optional;

public interface EnableBankingBalanceFetcher {
    Optional<Long> fetchBalanceCents(String bankAccountUid);
}
