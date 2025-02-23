package de.flur4.roomiefunds.domain.api.log.impl;

import de.flur4.roomiefunds.domain.api.log.GetLog;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.models.log.LogEntryDto;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class LogService implements GetLog {
    final LogRepository logRepository;

    @Override
    public List<LogEntryDto> getLogEntries() {
        return logRepository.getAllLogEntries();
    }
}
