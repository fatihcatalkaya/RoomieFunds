package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.enablebanking.BankTransactionDto;
import de.flur4.roomiefunds.models.enablebanking.BankTransactionEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository {
    int insertTransactions(long sessionId, List<BankTransactionDto> transactions);

    int deleteTransactionsInRange(long sessionId, LocalDate dateFrom, LocalDate dateTo);

    long computeTransactionSum(long sessionId);

    Optional<LocalDate> getLastBookingDate(long sessionId);

    List<BankTransactionEntity> getTransactionsBySession(long sessionId);
}
