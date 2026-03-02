package de.flur4.roomiefunds.models.group;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupDto(@NotBlank String name, String keycloakGroupId) {}
