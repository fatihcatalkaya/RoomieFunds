package de.flur4.roomiefunds.models.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.Optional;

public record UpdateTransactionDto(Optional<@NotNull @Positive Long> sourceAccountId,
                                   Optional<@NotNull @Positive Long> targetAccountId,
                                   Optional<@NotNull Integer> amount,
                                   Optional<@NotNull LocalDate> valueDate,
                                   Optional<@NotNull String> description) {
}
