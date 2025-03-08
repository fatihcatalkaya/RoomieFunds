package de.flur4.roomiefunds.domain.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import de.flur4.roomiefunds.models.log.LogEntryDto;

import java.util.List;

public interface LogRepository {
    void insertLogEntry(ModifyingPersonDto modifyingPersonDto, InsertLogEntryDto entryDto) throws JsonProcessingException;

    List<LogEntryDto> getAllLogEntries();

    List<LogEntryDto> getLogEntriesByTable(String table);

    List<LogEntryDto> getLogEntriesByTableAndObjectId(String table, String objectId);
}
