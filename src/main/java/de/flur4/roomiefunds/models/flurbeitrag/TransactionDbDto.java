package de.flur4.roomiefunds.models.flurbeitrag;

import java.time.LocalDate;

public record TransactionDbDto(long id,
                               long sourceAccountId,
                               long targetAccountId,
                               int amount,
                               LocalDate valueDate,
                               String description) {
}
