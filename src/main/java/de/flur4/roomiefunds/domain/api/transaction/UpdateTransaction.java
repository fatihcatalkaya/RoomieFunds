package de.flur4.roomiefunds.domain.api.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.transaction.Transaction;
import de.flur4.roomiefunds.models.transaction.UpdateTransactionDto;

public interface UpdateTransaction {
    Transaction updateTransaction(ModifyingPersonDto modifyingPerson, long transactionId, UpdateTransactionDto updateTransactionDto) throws TransactionNotFoundException, JsonProcessingException;
}
