package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.log.GetLog;
import de.flur4.roomiefunds.models.log.LogEntryDto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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

    @GET
    @Path("/{table_name:\\w+}")
    public List<LogEntryDto> getLogForTable(@PathParam("table_name") String tableName){
        return getLog.getLogEntriesByTable(tableName);
    }

    @GET
    @Path("/{table_name:\\w+}/{object_id:\\w+}")
    public List<LogEntryDto> getLogForTableAndObjectId(@PathParam("table_name") String tableName,
                                                       @PathParam("object_id") String objectId) {
        return getLog.getLogEntriesByTableAndObjectId(tableName, objectId);
    }
}
