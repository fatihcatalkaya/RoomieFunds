package de.flur4.roomiefunds.domain.api.log;

import de.flur4.roomiefunds.models.log.LogEntryDto;

import java.util.List;

public interface GetLog {
    List<LogEntryDto> getLogEntries();
    List<LogEntryDto> getLogEntriesByTable(String table);
    List<LogEntryDto> getLogEntriesByTableAndObjectId(String table, String objectId);
}
