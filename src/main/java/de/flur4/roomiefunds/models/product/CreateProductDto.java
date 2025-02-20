package de.flur4.roomiefunds.models.product;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateProductDto(@NotEmpty String name, @PositiveOrZero int price) {
}
