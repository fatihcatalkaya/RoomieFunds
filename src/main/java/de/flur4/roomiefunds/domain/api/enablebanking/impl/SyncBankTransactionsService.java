package de.flur4.roomiefunds.domain.api.enablebanking.impl;

import de.flur4.roomiefunds.domain.api.enablebanking.EnableBankingAuthenticationRequiredRuntimeException;
import de.flur4.roomiefunds.domain.api.enablebanking.SyncBankTransactions;
import de.flur4.roomiefunds.domain.spi.BankTransactionRepository;
import de.flur4.roomiefunds.domain.spi.EnableBankingBalanceFetcher;
import de.flur4.roomiefunds.domain.spi.EnableBankingRepository;
import de.flur4.roomiefunds.domain.spi.EnableBankingTransactionFetcher;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingSession;
import de.flur4.roomiefunds.models.enablebanking.BankTransactionDto;
import de.flur4.roomiefunds.models.enablebanking.SyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@JBossLog
public class SyncBankTransactionsService implements SyncBankTransactions {
    final EnableBankingRepository enableBankingRepository;
    final BankTransactionRepository bankTransactionRepository;
    final EnableBankingTransactionFetcher transactionFetcher;
    final EnableBankingBalanceFetcher balanceFetcher;

    @Override
    public List<SyncResult> syncAllSessions() {
        var sessions = enableBankingRepository.getActiveSessions();
        List<SyncResult> results = new ArrayList<>();
        for (var session : sessions) {
            try {
                results.add(syncSession(session.id()));
            } catch (Exception e) {
                log.errorf("Unexpected error syncing session %d: %s", session.id(), e.getMessage());
                results.add(new SyncResult(session.id(), 0, 0, 0, null, null, null, "FAILED", e.getMessage()));
            }
        }
        return results;
    }

    @Override
    public SyncResult syncSession(long sessionId) {
        var sessionOpt = enableBankingRepository.getSession(sessionId);
        if (sessionOpt.isEmpty()) {
            return new SyncResult(sessionId, 0, 0, 0, null, null, null, "FAILED", "Session not found");
        }

        var session = sessionOpt.get();

        if (session.bankAccountUid() == null || session.bankAccountUid().isBlank()) {
            return new SyncResult(sessionId, 0, 0, 0, null, null, null, "FAILED", "Session not finished (no bank account linked)");
        }

        if (session.validUntil() != null && session.validUntil().isBefore(OffsetDateTime.now())) {
            updateSyncStatus(sessionId, null, "EXPIRED", "Session expired", null, null, null, null);
            return new SyncResult(sessionId, 0, 0, 0, null, null, null, "EXPIRED", "Session expired");
        }

        try {
            return doSync(session);
        } catch (EnableBankingAuthenticationRequiredRuntimeException e) {
            log.warnf("Authentication required for session %d (bank account UID: %s)", sessionId, e.getBankAccountUid());
            updateSyncStatus(sessionId, null, "AUTH_REQUIRED", e.getMessage(), null, null, null, null);
            return new SyncResult(sessionId, 0, 0, 0, null, null, null, "AUTH_REQUIRED", e.getMessage());
        } catch (Exception e) {
            log.errorf(e, "Failed to sync session %d", sessionId);
            updateSyncStatus(sessionId, null, "FAILED", e.getMessage(), null, null, null, null);
            return new SyncResult(sessionId, 0, 0, 0, null, null, null, "FAILED", e.getMessage());
        }
    }

    private SyncResult doSync(EnableBankingSession session) {
        long sessionId = session.id();
        LocalDate dateTo = LocalDate.now();

        // Determine dateFrom and fetch strategy based on whether we have existing transactions
        var lastBookingDate = bankTransactionRepository.getLastBookingDate(sessionId);

        List<BankTransactionDto> transactions;
        int deleted = 0;
        if (lastBookingDate.isPresent()) {
            // Subsequent sync: overlap by 2 days to catch late-booked transactions
            LocalDate dateFrom = lastBookingDate.get().minusDays(2);
            transactions = transactionFetcher.fetchTransactions(session.bankAccountUid(), dateFrom, dateTo);

            // Delete existing transactions in the overlap range, then re-insert.
            // This avoids false-positive dedup — Enable Banking does not guarantee
            // unique entry_reference or transaction_id values.
            deleted = bankTransactionRepository.deleteTransactionsInRange(sessionId, dateFrom, dateTo);
            log.infof("Overlap sync for session %d: deleted %d existing transactions in [%s, %s] before re-insert",
                    sessionId, deleted, dateFrom, dateTo);
        } else {
            // Initial sync: use "longest" strategy to fetch all available history
            LocalDate dateFrom = dateTo.minusYears(10);
            log.infof("Initial sync for session %d — using 'longest' strategy from %s", sessionId, dateFrom);
            transactions = transactionFetcher.fetchTransactionsLongest(session.bankAccountUid(), dateFrom);
        }

        int fetched = transactions.size();
        int inserted = bankTransactionRepository.insertTransactions(sessionId, transactions);

        // Fetch balance from API
        Long apiBalanceCents = null;
        String apiBalanceCurrency = null;
        var balanceOpt = balanceFetcher.fetchBalanceCents(session.bankAccountUid());
        if (balanceOpt.isPresent()) {
            apiBalanceCents = balanceOpt.get();
            apiBalanceCurrency = "EUR"; // Enable Banking typically returns EUR for German banks
        }

        // On initial sync, derive and store the opening balance:
        // opening_balance = API balance - sum(all transactions)
        boolean isInitialSync = lastBookingDate.isEmpty();
        if (isInitialSync && apiBalanceCents != null) {
            long transactionSum = bankTransactionRepository.computeTransactionSum(sessionId);
            long openingBalanceCents = apiBalanceCents - transactionSum;
            enableBankingRepository.setOpeningBalance(sessionId, openingBalanceCents);
            log.infof("Session %d: derived opening balance %d cents (API=%d, txSum=%d)",
                    sessionId, openingBalanceCents, apiBalanceCents, transactionSum);
        }

        // Compute local balance = opening balance + sum(transactions)
        long openingBalance = enableBankingRepository.getOpeningBalance(sessionId).orElse(0L);
        long transactionSum = bankTransactionRepository.computeTransactionSum(sessionId);
        long computedBalanceCents = openingBalance + transactionSum;

        // Compare balances
        Boolean balanceMatch = null;
        if (apiBalanceCents != null) {
            balanceMatch = apiBalanceCents.equals(computedBalanceCents);
            if (!balanceMatch) {
                log.warnf("Balance mismatch for session %d: API=%d, computed=%d (opening=%d, txSum=%d)",
                        sessionId, apiBalanceCents, computedBalanceCents, openingBalance, transactionSum);
            }
        }

        updateSyncStatus(sessionId, dateTo, "SUCCESS", null, apiBalanceCents, apiBalanceCurrency, computedBalanceCents, balanceMatch);

        return new SyncResult(sessionId, fetched, inserted, deleted, balanceMatch, apiBalanceCents, computedBalanceCents, "SUCCESS", null);
    }

    private void updateSyncStatus(long sessionId, LocalDate lastSyncedDate, String status, String errorMessage,
                                  Long apiBalanceCents, String apiBalanceCurrency, Long computedBalanceCents, Boolean balanceMatch) {
        enableBankingRepository.updateSyncStatus(
                sessionId, OffsetDateTime.now(), lastSyncedDate, status, errorMessage,
                apiBalanceCents, apiBalanceCurrency, computedBalanceCents, balanceMatch
        );
    }
}
