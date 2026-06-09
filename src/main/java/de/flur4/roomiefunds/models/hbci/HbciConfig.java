package de.flur4.roomiefunds.models.hbci;

import java.time.OffsetDateTime;

public record HbciConfig(long id, String blz, String username, long accountId, OffsetDateTime lastSyncedAt) {
}
