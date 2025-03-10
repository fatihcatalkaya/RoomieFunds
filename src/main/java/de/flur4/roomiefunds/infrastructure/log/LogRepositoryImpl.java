package de.flur4.roomiefunds.infrastructure.log;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.infrastructure.jooq.tables.Transaction;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import de.flur4.roomiefunds.models.log.LogEntryDto;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.jooq.DSLContext;
import org.jooq.JSONB;

import java.util.List;
import java.util.Locale;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.LOG;
import static de.flur4.roomiefunds.infrastructure.jooq.Tables.TRANSACTION;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.*;

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
            modifyingPersonString += " " + modifyingPersonDto.username().get();
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

    @Override
    public List<LogEntryDto> getAllLogEntries() {
        return jooq.select(
                        LOG.ID,
                        LOG.CREATED_AT,
                        LOG.OPERATION,
                        LOG.MODIFIED_TABLE_NAME,
                        LOG.MODIFIED_BY,
                        nvl(LOG.SUBJECT_BEFORE, JSONB.jsonb("{}")).cast(String.class),
                        nvl(LOG.SUBJECT_AFTER, JSONB.jsonb("{}")).cast(String.class)
                ).from(LOG)
                .orderBy(LOG.CREATED_AT.desc())
                .fetch(mapping(LogEntryDto::new));
    }

    @Override
    public List<LogEntryDto> getLogEntriesByTable(String table) {
        return jooq.select(
                        LOG.ID,
                        LOG.CREATED_AT,
                        LOG.OPERATION,
                        LOG.MODIFIED_TABLE_NAME,
                        LOG.MODIFIED_BY,
                        nvl(LOG.SUBJECT_BEFORE, JSONB.jsonb("{}")).cast(String.class),
                        nvl(LOG.SUBJECT_AFTER, JSONB.jsonb("{}")).cast(String.class)
                ).from(LOG)
                .where(LOG.MODIFIED_TABLE_NAME.eq(table))
                .orderBy(LOG.CREATED_AT.desc())
                .fetch(mapping(LogEntryDto::new));
    }

    @Override
    public List<LogEntryDto> getLogEntriesByTableAndObjectId(String table, String objectId) {
        return jooq.select(
                        LOG.ID,
                        LOG.CREATED_AT,
                        LOG.OPERATION,
                        LOG.MODIFIED_TABLE_NAME,
                        LOG.MODIFIED_BY,
                        nvl(LOG.SUBJECT_BEFORE, JSONB.jsonb("{}")).cast(String.class),
                        nvl(LOG.SUBJECT_AFTER, JSONB.jsonb("{}")).cast(String.class)
                ).from(LOG)
                .where(LOG.MODIFIED_TABLE_NAME.eq(table))
                .and(
                        jsonbGetAttribute(LOG.SUBJECT_BEFORE, "id").cast(String.class).eq(objectId).or(
                                jsonbGetAttribute(LOG.SUBJECT_AFTER, "id").cast(String.class).eq(objectId)
                        )
                )
                .orderBy(LOG.CREATED_AT.desc())
                .fetch(mapping(LogEntryDto::new));
    }

    @Override
    public List<LogEntryDto> getTransactionLogEntriesByAccountId(String accountId) {
        return jooq.select(
                        LOG.ID,
                        LOG.CREATED_AT,
                        LOG.OPERATION,
                        LOG.MODIFIED_TABLE_NAME,
                        LOG.MODIFIED_BY,
                        nvl(LOG.SUBJECT_BEFORE, JSONB.jsonb("{}")).cast(String.class),
                        nvl(LOG.SUBJECT_AFTER, JSONB.jsonb("{}")).cast(String.class)
                ).from(LOG)
                .where(LOG.MODIFIED_TABLE_NAME.eq("transaction"))
                .and(
                        jsonbGetAttribute(LOG.SUBJECT_BEFORE, "sourceAccountId").cast(String.class).eq(accountId)
                                .or(jsonbGetAttribute(LOG.SUBJECT_BEFORE, "targetAccountId").cast(String.class).eq(accountId))
                                .or(jsonbGetAttribute(LOG.SUBJECT_AFTER, "sourceAccountId").cast(String.class).eq(accountId))
                                .or(jsonbGetAttribute(LOG.SUBJECT_AFTER, "targetAccountId").cast(String.class).eq(accountId))
                )
                .orderBy(LOG.CREATED_AT.desc())
                .fetch(mapping(LogEntryDto::new));
    }
}
