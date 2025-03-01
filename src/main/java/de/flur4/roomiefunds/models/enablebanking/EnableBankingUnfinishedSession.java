package de.flur4.roomiefunds.models.enablebanking;

import java.time.OffsetDateTime;
import java.util.List;

public record EnableBankingUnfinishedSession(long unfinishedSessionId,
                                             OffsetDateTime validUntil,
                                             String bankName,
                                             List<EnableBankingAccountDto> accounts) {
}
