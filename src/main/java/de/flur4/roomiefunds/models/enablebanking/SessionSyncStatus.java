package de.flur4.roomiefunds.models.enablebanking;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SessionSyncStatus(
        long sessionId,
        OffsetDateTime lastSyncedAt,
        LocalDate lastSyncedDate,
        String lastSyncStatus,
        String lastSyncErrorMessage,
        Long apiBalanceCents,
        String apiBalanceCurrency,
        Long computedBalanceCents,
        Boolean balanceMatch
) {}
