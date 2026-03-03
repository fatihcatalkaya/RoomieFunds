package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.enablebanking.BankTransactionDto;

import java.time.LocalDate;
import java.util.List;

public interface EnableBankingTransactionFetcher {
    List<BankTransactionDto> fetchTransactions(String bankAccountUid, LocalDate dateFrom, LocalDate dateTo);

    /**
     * Fetch transactions using the "longest" strategy, which retrieves all available history
     * from the earliest point onward. Recommended for initial syncs.
     */
    List<BankTransactionDto> fetchTransactionsLongest(String bankAccountUid, LocalDate dateFrom);
}
