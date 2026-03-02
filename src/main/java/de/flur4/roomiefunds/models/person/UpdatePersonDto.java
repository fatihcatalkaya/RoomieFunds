package de.flur4.roomiefunds.models.person;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

public record UpdatePersonDto(
        Optional<@NotBlank String> firstName,
        Optional<@NotBlank String> lastName,
        Optional<@NotBlank String> room,
        Optional<@NotNull Boolean> paysFloorFees,
        Optional<@NotNull Boolean> printOnProductTallyList,
        Optional<@Email String> email,
        Optional<@NotNull Boolean> emailAccountStatement
) {
}
