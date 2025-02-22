package de.flur4.roomiefunds.models.product;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Optional;

public record UpdateProductDto(
        Optional<@NotEmpty String> name,
        Optional<@PositiveOrZero Integer> price,
        Optional<@NotNull Boolean> print) {
}
