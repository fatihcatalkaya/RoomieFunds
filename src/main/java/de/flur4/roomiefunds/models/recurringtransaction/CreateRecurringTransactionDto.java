package de.flur4.roomiefunds.models.recurringtransaction;

import jakarta.validation.constraints.NotBlank;

public record CreateRecurringTransactionDto(int amount,
                                            long sourceAccountId,
                                            long targetAccountId,
                                            int valueDayOfMonth,
                                            @NotBlank String name,
                                            @NotBlank String transactionDescription) {
}
