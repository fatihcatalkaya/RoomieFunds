package de.flur4.roomiefunds.models.group;

import jakarta.validation.constraints.NotBlank;
import java.util.Optional;

public record UpdateGroupDto(Optional<@NotBlank String> name, Optional<String> keycloakGroupId) {}
