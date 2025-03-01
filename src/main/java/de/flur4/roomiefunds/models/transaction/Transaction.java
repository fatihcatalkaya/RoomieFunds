package de.flur4.roomiefunds.models.transaction;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record Transaction(long id,
                          long sourceAccountId,
                          String sourceAccountName,
                          boolean sourceAccountActive,
                          long targetAccountId,
                          String targetAccountName,
                          boolean targetAccountActive,
                          int amount,
                          OffsetDateTime createdAt,
                          LocalDate valueDate,
                          String description,
                          boolean hasReceipt) {
}
