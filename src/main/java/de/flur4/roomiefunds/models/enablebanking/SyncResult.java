package de.flur4.roomiefunds.models.enablebanking;

public record SyncResult(
        long sessionId,
        int transactionsFetched,
        int transactionsInserted,
        int transactionsDeleted,
        Boolean balanceMatch,
        Long apiBalanceCents,
        Long computedBalanceCents,
        String status,
        String errorMessage
) {}
