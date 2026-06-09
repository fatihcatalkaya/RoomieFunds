package de.flur4.roomiefunds.models.hbci;

import java.time.LocalDate;
import java.util.Objects;

public record HbciTransactionEntry(String camtId,
                                   LocalDate valueDate,
                                   int amountCents,
                                   String counterpartyIban,
                                   String usage) {
    public HbciTransactionEntry {
        Objects.requireNonNull(camtId, "camtId must not be null");
    }
}
