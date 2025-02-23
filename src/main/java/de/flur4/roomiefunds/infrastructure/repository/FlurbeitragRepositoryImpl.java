package de.flur4.roomiefunds.infrastructure.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.flur4.roomiefunds.domain.spi.FlurbeitragRepository;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.flurbeitrag.TransactionDbDto;
import de.flur4.roomiefunds.models.person.Person;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.*;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.*;

@ApplicationScoped
@RequiredArgsConstructor
public class FlurbeitragRepositoryImpl implements FlurbeitragRepository {
    final static String SETTINGS_FLURBEITRAG_AMOUNT = "flurbeitrag_amount";
    final static DateTimeFormatter GERMAN_DTF = DateTimeFormatter.ofPattern("MM/yyyy", Locale.GERMANY);
    final static String FLURBEITRAG_DEFAULT_TRANSACTION_TEXT = "Flurbeitrag %s";
    final DSLContext jooq;
    final ObjectMapper objectMapper;

    @Override
    public long getFlurbeitrag() {
        return jooq.select(SETTINGS.VALUE_INT)
                .from(SETTINGS)
                .where(SETTINGS.SETTING_KEY.eq(SETTINGS_FLURBEITRAG_AMOUNT))
                .fetchOne()
                .value1();
    }

    @Override
    public void setFlurbeitrag(long flurbeitrag) {
        jooq.update(SETTINGS)
                .set(SETTINGS.VALUE_INT, flurbeitrag)
                .where(SETTINGS.SETTING_KEY.eq(SETTINGS_FLURBEITRAG_AMOUNT))
                .execute();
    }

    @Override
    public boolean existsFlurbeitragForMonthInYear(int month, int year) {
        return jooq.select(exists(
                jooq.selectOne()
                        .from(FLURBEITRAG_SCHEDULER_LOG)
                        .where(month(FLURBEITRAG_SCHEDULER_LOG.CREATED_AT).eq(month))
                        .and(year(FLURBEITRAG_SCHEDULER_LOG.CREATED_AT).eq(year))
        )).fetchOne().value1().booleanValue();
    }

    @Override
    public void createFlurbeitragTransactions(ModifyingPersonDto modifyingPerson,
                                              List<Person> personList,
                                              long flurbeitrag,
                                              long flurkontoAccountId,
                                              LocalDate date) {
        // Construct message strings
        final String description = FLURBEITRAG_DEFAULT_TRANSACTION_TEXT.formatted(date.format(GERMAN_DTF));
        final String modifyingPersonString = (modifyingPerson.ssoId() + " " + (modifyingPerson.username().orElse(""))).trim();

        jooq.transaction(tx -> {
            personList.stream().map(person ->
                    jooq.insertInto(TRANSACTION)
                            .columns(
                                    TRANSACTION.SOURCE_ACCOUNT_ID,
                                    TRANSACTION.TARGET_ACCOUNT_ID,
                                    TRANSACTION.AMOUNT,
                                    TRANSACTION.VALUE_DATE,
                                    TRANSACTION.DESCRIPTION
                            ).values(
                                    person.accountId(),
                                    flurkontoAccountId,
                                    (int) flurbeitrag,
                                    date,
                                    description
                            ).returningResult(
                                    TRANSACTION.ID,
                                    TRANSACTION.SOURCE_ACCOUNT_ID,
                                    TRANSACTION.TARGET_ACCOUNT_ID,
                                    TRANSACTION.AMOUNT,
                                    TRANSACTION.VALUE_DATE,
                                    TRANSACTION.DESCRIPTION
                            ).fetchOne(mapping(TransactionDbDto::new))
            ).forEach(transaction -> {
                        String jsonTransaction;
                        try {
                            jsonTransaction = objectMapper.writeValueAsString(transaction);
                        } catch (JsonProcessingException e) {
                            jsonTransaction = "{}";
                        }
                        jooq.insertInto(LOG).columns(
                                LOG.OPERATION,
                                LOG.MODIFIED_TABLE_NAME,
                                LOG.MODIFIED_BY,
                                LOG.SUBJECT_BEFORE,
                                LOG.SUBJECT_AFTER
                        ).values(
                                LogOperations.create,
                                "transaction",
                                modifyingPersonString,
                                JSONB.jsonb(jsonTransaction),
                                null
                        ).execute();
                    }
            );

            // Add log entry
            tx.dsl().insertInto(FLURBEITRAG_SCHEDULER_LOG)
                    .columns(FLURBEITRAG_SCHEDULER_LOG.CREATED_AT)
                    .values(date)
                    .execute();
        });
    }
}
