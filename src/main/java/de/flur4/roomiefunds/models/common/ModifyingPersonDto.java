package de.flur4.roomiefunds.models.common;

import java.util.Optional;

public record ModifyingPersonDto(String ssoId, Optional<String> username) {
}
