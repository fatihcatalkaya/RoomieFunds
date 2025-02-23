package de.flur4.roomiefunds.domain.api.recurringtransaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.recurringtransaction.CreateRecurringTransactionDto;
import de.flur4.roomiefunds.models.recurringtransaction.GetRecurringTransactionDto;

public interface CreateRecurringTransaction {
    GetRecurringTransactionDto createRecurringTransaction(ModifyingPersonDto modifyingPerson,
                                                          CreateRecurringTransactionDto createRecurringTransaction) throws JsonProcessingException;

    void createScheduledTransactions();
}
