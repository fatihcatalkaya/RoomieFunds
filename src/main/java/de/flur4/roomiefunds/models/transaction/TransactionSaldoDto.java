package de.flur4.roomiefunds.models.transaction;

public record TransactionSaldoDto(
        Transaction transaction,
        long saldo,
        String[] sourceAccountNameParts,
        String[] targetAccountNameParts
) {
}
