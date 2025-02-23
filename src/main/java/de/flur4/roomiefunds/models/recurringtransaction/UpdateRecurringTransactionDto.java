package de.flur4.roomiefunds.models.recurringtransaction;

import jakarta.validation.constraints.*;

import java.util.Optional;

public record UpdateRecurringTransactionDto(Optional<@NotNull @PositiveOrZero Integer> amount,
                                            Optional<@NotNull @Positive Long> sourceAccountId,
                                            Optional<@NotNull @Positive Long> targetAccountId,
                                            Optional<@NotNull @Positive @Max(31) Integer> valueDayOfMonth,
                                            Optional<@NotBlank String> name,
                                            Optional<@NotBlank String> transactionDescription) {
}
