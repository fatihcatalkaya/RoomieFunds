package de.flur4.roomiefunds.domain.api.recurringtransaction;

public class RecurringTransactionNotFoundException extends Exception {
    public RecurringTransactionNotFoundException(long recurringTransactionId) {
      super("Could not find recurring transaction with id %d".formatted(recurringTransactionId));
    }
}
