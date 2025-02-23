package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.transaction.CreateTransactionDto;
import de.flur4.roomiefunds.models.transaction.Transaction;
import de.flur4.roomiefunds.models.transaction.UpdateTransactionDto;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    boolean accountHasTransactions(long accountId);
    Optional<Transaction> getTransactionById(long transactionId);
    List<Transaction> getTransactionsByAccountId(long accountId);
    void deleteTransaction(long transactionId);
    Transaction createTransaction(CreateTransactionDto createTransactionDto);
    Transaction updateTransaction(long transactionId, UpdateTransactionDto updateTransactionDto);
}
