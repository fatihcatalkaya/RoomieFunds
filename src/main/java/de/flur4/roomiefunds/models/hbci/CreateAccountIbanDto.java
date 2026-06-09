package de.flur4.roomiefunds.models.hbci;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateAccountIbanDto(@Positive long accountId,
                                   @NotBlank @Pattern(regexp = "[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}",
                                           message = "must be a valid IBAN") String iban) {
}
