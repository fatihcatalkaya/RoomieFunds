package de.flur4.roomiefunds.models.enablebanking;

import java.time.OffsetDateTime;

public record EnableBankingSession(long id,
                                   OffsetDateTime validUntil,
                                   String bankName,
                                   String bankAccountIban,
                                   String bankAccountUid,
                                   Long accountId) {
}
