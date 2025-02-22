package de.flur4.roomiefunds.models.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

public record UpdateAccountDto(
        Optional<@NotBlank String> name,
        Optional<@NotNull Boolean> active
) {
}
