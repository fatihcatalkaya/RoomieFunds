package de.flur4.roomiefunds.models.flurbeitrag;

import jakarta.validation.constraints.PositiveOrZero;

public record Flurbeitrag(@PositiveOrZero long flurbeitrag) {
}
