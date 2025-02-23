package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.recurringtransaction.CreateRecurringTransactionDto;
import de.flur4.roomiefunds.models.recurringtransaction.GetRecurringTransactionDto;
import de.flur4.roomiefunds.models.recurringtransaction.UpdateRecurringTransactionDto;

import java.util.List;
import java.util.Optional;

public interface RecurringTransactionRepository {
    Optional<GetRecurringTransactionDto> getRecurringTransactionById(long recurringTransactionId);
    List<GetRecurringTransactionDto> getAllRecurringTransactions();
    GetRecurringTransactionDto createRecurringTransaction(CreateRecurringTransactionDto createRecurringTransaction);
    GetRecurringTransactionDto updateRecurringTransaction(long recurringTransactionId, UpdateRecurringTransactionDto updateRecurringTransaction);
    void deleteRecurringTransaction(long recurringTransactionId);
}
