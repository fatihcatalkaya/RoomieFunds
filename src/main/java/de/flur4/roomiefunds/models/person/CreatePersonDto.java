package de.flur4.roomiefunds.models.person;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePersonDto(@NotBlank String name,
                              @NotBlank String room,
                              @NotNull boolean paysFloorFees,
                              @NotNull boolean printOnProductTallyList) {
}
