package de.flur4.roomiefunds.domain.api.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.transaction.CreateTransactionDto;
import de.flur4.roomiefunds.models.transaction.Transaction;

public interface CreateTransaction {
    Transaction createTransaction(ModifyingPersonDto modifyingPerson, CreateTransactionDto createTransactionDto) throws JsonProcessingException;
}
