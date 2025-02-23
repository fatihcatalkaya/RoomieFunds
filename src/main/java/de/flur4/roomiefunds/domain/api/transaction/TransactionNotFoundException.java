package de.flur4.roomiefunds.domain.api.transaction;

public class TransactionNotFoundException extends Exception {
    public TransactionNotFoundException(long productId) {
        super("Could not find transaction with id %d".formatted(productId));
    }
}
