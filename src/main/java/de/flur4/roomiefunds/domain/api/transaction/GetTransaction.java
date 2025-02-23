package de.flur4.roomiefunds.domain.api.transaction;

import de.flur4.roomiefunds.models.transaction.Transaction;
import de.flur4.roomiefunds.models.transaction.TransactionSaldoDto;

import java.util.List;
import java.util.Optional;

public interface GetTransaction {
    Optional<Transaction> getTransaction(long transactionId);
    List<TransactionSaldoDto> getTransactionsForAccount(long accountId);
}
