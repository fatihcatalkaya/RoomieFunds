package de.flur4.roomiefunds.models.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateTransactionDto(@Positive long sourceAccountId,
                                   @Positive long targetAccountId,
                                   int amount,
                                   LocalDate valueDate,
                                   @NotNull String description) {
}
