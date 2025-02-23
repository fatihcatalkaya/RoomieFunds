package de.flur4.roomiefunds.domain.api.recurringtransaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.recurringtransaction.GetRecurringTransactionDto;
import de.flur4.roomiefunds.models.recurringtransaction.UpdateRecurringTransactionDto;

public interface UpdateRecurringTransaction {
    GetRecurringTransactionDto updateRecurringTransaction(ModifyingPersonDto modifyingPerson,
                                                          long transactionId,
                                                          UpdateRecurringTransactionDto updateRecurringTransaction) throws JsonProcessingException, RecurringTransactionNotFoundException;
}
