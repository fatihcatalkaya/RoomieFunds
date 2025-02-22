package de.flur4.roomiefunds.models.person;

import java.util.Optional;

public record UpdatePersonDto(
        Optional<String> name,
        Optional<String> room,
        Optional<Boolean> paysFloorFees
) {
}
