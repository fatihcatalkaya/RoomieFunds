package de.flur4.roomiefunds.models.enablebanking;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record BankTransactionEntity(
        long id,
        long sessionId,
        String entryReference,
        String transactionId,
        int amountCents,
        String currency,
        String creditDebitIndicator,
        LocalDate bookingDate,
        LocalDate valueDate,
        String creditorName,
        String debtorName,
        String creditorIban,
        String debtorIban,
        List<String> remittanceInformation,
        String status,
        OffsetDateTime createdAt
) {}
