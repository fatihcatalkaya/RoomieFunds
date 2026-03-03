package de.flur4.roomiefunds.models.enablebanking;

import java.util.List;

public record BankTransactionsResult(
        List<BankTransactionDto> transactions,
        String bankName,
        String iban,
        Long linkedAccountId
) {}
