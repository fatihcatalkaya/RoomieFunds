package de.flur4.roomiefunds.models.log;

import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;

import java.time.OffsetDateTime;

public record LogEntryDto(long id,
                          OffsetDateTime createdAt,
                          LogOperations logOperation,
                          String modifiedTableName,
                          String modifiedBy,
                          String subjectBeforeJson,
                          String subjectAfterJson) {
}
