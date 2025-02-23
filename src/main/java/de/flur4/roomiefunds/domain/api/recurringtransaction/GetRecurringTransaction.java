package de.flur4.roomiefunds.domain.api.recurringtransaction;

import de.flur4.roomiefunds.models.recurringtransaction.GetRecurringTransactionDto;

import java.util.List;
import java.util.Optional;

public interface GetRecurringTransaction {
    List<GetRecurringTransactionDto> getRecurringTransactions();
    Optional<GetRecurringTransactionDto> getRecurringTransaction(long recurringTransactionId);
}
