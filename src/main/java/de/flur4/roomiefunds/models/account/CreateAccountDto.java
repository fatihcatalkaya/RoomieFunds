package de.flur4.roomiefunds.models.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountDto(@NotBlank String name, @NotNull boolean active) {
}
