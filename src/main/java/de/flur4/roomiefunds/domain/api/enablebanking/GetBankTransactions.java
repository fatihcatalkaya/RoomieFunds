package de.flur4.roomiefunds.domain.api.enablebanking;

import de.flur4.roomiefunds.models.enablebanking.BankTransactionsResult;

import java.time.LocalDate;

public interface GetBankTransactions {
    BankTransactionsResult getBankTransactions(long sessionId, LocalDate dateFrom, LocalDate dateTo) throws SessionNotFoundException, SessionExpiredException;
}
