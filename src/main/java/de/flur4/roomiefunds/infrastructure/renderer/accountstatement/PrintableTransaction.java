package de.flur4.roomiefunds.infrastructure.renderer.accountstatement;

public record PrintableTransaction(
        String date,
        String description,
        String bookingTarget,
        String amount,
        String balance,
        boolean isBalanceNegative
) {
}
