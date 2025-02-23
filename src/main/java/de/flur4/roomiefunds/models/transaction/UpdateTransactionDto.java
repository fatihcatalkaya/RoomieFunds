package de.flur4.roomiefunds.models.transaction;

import java.time.LocalDate;
import java.util.Optional;

public record UpdateTransactionDto(Optional<Long> sourceAccountId,
                                   Optional<Long> targetAccountId,
                                   Optional<Integer> amount,
                                   Optional<LocalDate> valueDate,
                                   Optional<String> description) {
}
