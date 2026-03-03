package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.enablebanking.BankTransactionDto;

import java.time.LocalDate;
import java.util.List;

public interface EnableBankingTransactionFetcher {
    List<BankTransactionDto> fetchTransactions(String bankAccountUid, LocalDate dateFrom, LocalDate dateTo);
}
