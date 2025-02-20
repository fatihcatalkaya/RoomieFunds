package de.flur4.roomiefunds.domain.spi;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;

public interface LogRepository {
    void insertLogEntry(ModifyingPersonDto modifyingPersonDto, InsertLogEntryDto entryDto) throws JsonProcessingException;
}
