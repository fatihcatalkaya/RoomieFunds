package de.flur4.roomiefunds.domain.api.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;

public interface DeleteTransaction {
    void deleteTransaction(ModifyingPersonDto modifyingPerson, long transactionId) throws TransactionNotFoundException, JsonProcessingException;
}
