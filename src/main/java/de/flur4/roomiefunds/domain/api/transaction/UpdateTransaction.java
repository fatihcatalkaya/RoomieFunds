package de.flur4.roomiefunds.domain.api.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.transaction.Transaction;
import de.flur4.roomiefunds.models.transaction.UpdateTransactionDto;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;

public interface UpdateTransaction {
    Transaction updateTransaction(ModifyingPersonDto modifyingPerson, long transactionId, UpdateTransactionDto updateTransactionDto) throws TransactionNotFoundException, JsonProcessingException, IllegalArgumentException;

    void setTransactionReceipt(ModifyingPersonDto modifyingPerson, long transactionId, FileUpload fileUpload) throws TransactionNotFoundException, IOException;
}
