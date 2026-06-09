package de.flur4.roomiefunds.models.hbci;

import java.time.OffsetDateTime;

// Internal pipeline object — not intended for equality comparison (byte[] fields)
public record HbciCredentials(String blz,
                              String username,
                              String decryptedPin,
                              byte[] passportBytes,
                              long accountId,
                              OffsetDateTime lastSyncedAt) {
}
