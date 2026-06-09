package de.flur4.roomiefunds.models.hbci;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SaveHbciConfigDto(@NotBlank String blz,
                                @NotBlank String username,
                                @NotBlank String pin,
                                @Positive long accountId) {
}
