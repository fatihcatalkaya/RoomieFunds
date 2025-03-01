package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.transaction.CreateTransactionDto;
import de.flur4.roomiefunds.models.transaction.ReceiptDto;
import de.flur4.roomiefunds.models.transaction.Transaction;
import de.flur4.roomiefunds.models.transaction.UpdateTransactionDto;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    boolean accountHasTransactions(long accountId);

    Optional<Transaction> getTransactionById(long transactionId);

    List<Transaction> getTransactionsByAccountId(long accountId);

    void deleteTransaction(long transactionId);

    Transaction createTransaction(CreateTransactionDto createTransactionDto);

    Transaction updateTransaction(long transactionId, UpdateTransactionDto updateTransactionDto);

    Optional<ReceiptDto> getTransactionReceipt(long transactionId);

    Transaction deleteTransactionReceipt(long transactionId);

    Transaction setTransactionReceipt(long transactionId, FileUpload fileUpload) throws IOException;
}
