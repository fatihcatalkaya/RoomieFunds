package de.flur4.roomiefunds.domain.spi;

public interface TransactionRepository {
    boolean accountHasTransactions(long accountId);
}
