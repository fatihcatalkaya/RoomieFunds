package de.flur4.roomiefunds.domain.api.recurringtransaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;

public interface DeleteRecurringTransaction {
    void deleteRecurringTransaction(ModifyingPersonDto modifyingPerson,
                                    long transactionId) throws JsonProcessingException, RecurringTransactionNotFoundException;
}
