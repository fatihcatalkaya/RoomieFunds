package de.flur4.roomiefunds.models.enablebanking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FinishSessionRequest(
        @NotBlank String bankAccountIban,
        @NotBlank String bankAccountUid,
        @NotNull @Positive Long accountId) {
}
