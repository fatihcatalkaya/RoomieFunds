package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.log.GetLog;
import de.flur4.roomiefunds.domain.api.log.impl.LogService;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class LogContext {
    @Produces
    @ApplicationScoped
    public GetLog getLog(LogRepository logRepository) {
        return new LogService(logRepository);
    }
}
