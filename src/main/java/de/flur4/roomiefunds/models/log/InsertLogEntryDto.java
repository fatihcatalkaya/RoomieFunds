package de.flur4.roomiefunds.models.log;

import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;

import java.util.Optional;

public record InsertLogEntryDto(
        LogOperations operation,
        String modifiedTable,
        Optional<Object> subjectBefore,
        Optional<Object> subjectAfter
) {
}
