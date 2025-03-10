package de.flur4.roomiefunds.models.person;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

public record CreatePersonDto(@NotBlank String name,
                              @NotBlank String room,
                              Optional<@Email String> email,
                              @NotNull boolean paysFloorFees,
                              @NotNull boolean printOnProductTallyList,
                              @NotNull boolean emailAccountStatement) {
}
