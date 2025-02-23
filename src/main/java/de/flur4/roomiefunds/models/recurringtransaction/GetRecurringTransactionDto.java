package de.flur4.roomiefunds.models.recurringtransaction;

public record GetRecurringTransactionDto(long id,
                                         int amount,
                                         long sourceAccountId,
                                         String sourceAccountName,
                                         boolean sourceAccountActive,
                                         long targetAccountId,
                                         String targetAccountName,
                                         boolean targetAccountActive,
                                         int valueDayOfMonth,
                                         String name,
                                         String transactionDescription) {
}
