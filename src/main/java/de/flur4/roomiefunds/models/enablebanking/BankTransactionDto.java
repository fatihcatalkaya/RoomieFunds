package de.flur4.roomiefunds.models.enablebanking;

import java.time.LocalDate;
import java.util.List;

public record BankTransactionDto(
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
        String status
) {}
