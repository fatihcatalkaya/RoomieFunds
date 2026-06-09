package de.flur4.roomiefunds.models.hbci;

import java.util.List;

// Internal pipeline object — not intended for equality comparison (byte[] fields)
public record HbciFetchResult(List<HbciTransactionEntry> entries, byte[] updatedPassportBytes) {
}
