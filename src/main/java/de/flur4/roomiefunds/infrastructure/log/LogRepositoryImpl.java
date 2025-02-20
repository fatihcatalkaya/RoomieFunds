package de.flur4.roomiefunds.infrastructure.log;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.jooq.DSLContext;
import org.jooq.JSONB;

import java.util.Locale;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.LOG;

@ApplicationScoped
@JBossLog
@RequiredArgsConstructor
public class LogRepositoryImpl implements LogRepository {

    static final String LOG_MESSAGE_FORMAT = "%s performed operation '%s' on table '%s' (before: '%s', after: '%s')";
    final DSLContext jooq;
    final ObjectMapper objectMapper;

    @Override
    public void insertLogEntry(ModifyingPersonDto modifyingPersonDto, InsertLogEntryDto entryDto) throws JsonProcessingException {
        // Construct modifying person string
        String modifyingPersonString = modifyingPersonDto.ssoId();
        if (modifyingPersonDto.username().isPresent()) {
            modifyingPersonString += modifyingPersonDto.username().get();
        }

        String objectBeforeJson = null;
        String objectAfterJson = null;
        if (entryDto.subjectBefore().isPresent()) {
            objectBeforeJson = objectMapper.writeValueAsString(entryDto.subjectBefore().get());
        }
        if (entryDto.subjectAfter().isPresent()) {
            objectAfterJson = objectMapper.writeValueAsString(entryDto.subjectAfter().get());
        }

        jooq.insertInto(LOG).columns(
                LOG.OPERATION,
                LOG.MODIFIED_TABLE_NAME,
                LOG.MODIFIED_BY,
                LOG.SUBJECT_BEFORE,
                LOG.SUBJECT_AFTER
        ).values(
                entryDto.operation(),
                entryDto.modifiedTable().toLowerCase(Locale.getDefault()),
                modifyingPersonString,
                JSONB.jsonbOrNull(objectBeforeJson),
                JSONB.jsonbOrNull(objectAfterJson)
        ).execute();

        log.info(LOG_MESSAGE_FORMAT.formatted(
                modifyingPersonString,
                entryDto.operation().getLiteral().toLowerCase(Locale.getDefault()),
                entryDto.modifiedTable().toLowerCase(Locale.getDefault()),
                objectBeforeJson == null ? "" : objectBeforeJson,
                objectAfterJson == null ? "" : objectAfterJson
        ));
    }
}
