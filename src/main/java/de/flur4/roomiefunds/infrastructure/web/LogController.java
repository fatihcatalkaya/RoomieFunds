package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.log.GetLog;
import de.flur4.roomiefunds.models.log.LogEntryDto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

import java.util.List;

@Path("/api/log")
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class LogController {
    final GetLog getLog;

    @GET
    public List<LogEntryDto> getLog() {
        return getLog.getLogEntries();
    }
}
