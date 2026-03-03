package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.api.enablebanking.EnableBankingClientException;
import de.flur4.roomiefunds.domain.spi.EnableBankingRepository;
import de.flur4.roomiefunds.infrastructure.webclient.enablebanking.EnableBankingClient;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingAccountDto;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingSession;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingUnfinishedSession;
import de.flur4.roomiefunds.models.enablebanking.FinishSessionRequest;
import de.flur4.roomiefunds.models.enablebanking.SessionSyncStatus;
import de.flur4.roomiefunds.models.webclient.enablebanking.AuthorizeSessionResponse;
import de.flur4.roomiefunds.models.webclient.enablebanking.CashAccountType;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.tools.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.ACCOUNT;
import static de.flur4.roomiefunds.infrastructure.jooq.Tables.ENABLE_BANKING_SESSION;
import static de.flur4.roomiefunds.infrastructure.jooq.Tables.ENABLE_BANKING_SESSION_ACCOUNT;
import static org.jooq.Records.mapping;

@RequiredArgsConstructor
@ApplicationScoped
@JBossLog
public class EnableBankingRepositoryImpl implements EnableBankingRepository {
    final DSLContext jooq;

    @RestClient
    EnableBankingClient enableBankingClient;

    public void storeNewSession(AuthorizeSessionResponse dto) {
        jooq.transaction(tx -> {
            // Write session into database
            var sessionId = dto.sessionId();
            var validUntil = dto.access().validUntil();
            var bankName = dto.aspsp().name();

            var dbId = tx.dsl().insertInto(ENABLE_BANKING_SESSION).columns(
                            ENABLE_BANKING_SESSION.SESSION_ID,
                            ENABLE_BANKING_SESSION.VALID_UNTIL,
                            ENABLE_BANKING_SESSION.BANK_NAME
                    ).values(
                            sessionId,
                            validUntil,
                            bankName
                    ).returningResult(ENABLE_BANKING_SESSION.ID)
                    .fetchOne()
                    .value1();

            // Store each account associated with this session
            for (var accountResource : dto.accounts()) {
                if (accountResource.cashAccountType() != CashAccountType.CACC) {
                    continue;
                }
                String accountUid = accountResource.uid();
                String accountIban = null;
                if (accountResource.accountId() != null) {
                    accountIban = accountResource.accountId().iban();
                    if (StringUtils.isEmpty(accountIban) && accountResource.accountId().other() != null) {
                        accountIban = accountResource.accountId().other().identification();
                    }
                }

                tx.dsl().insertInto(ENABLE_BANKING_SESSION_ACCOUNT)
                        .columns(
                                ENABLE_BANKING_SESSION_ACCOUNT.SESSION_ID,
                                ENABLE_BANKING_SESSION_ACCOUNT.ACCOUNT_UID,
                                ENABLE_BANKING_SESSION_ACCOUNT.ACCOUNT_IBAN
                        ).values(dbId, accountUid, accountIban)
                        .execute();
            }
        });
    }

    public Optional<EnableBankingUnfinishedSession> getUnfinishedSession(long sessionId) {
        AtomicReference<Optional<EnableBankingUnfinishedSession>> stuff = new AtomicReference<>();
        jooq.transaction(tx -> {
            var unfinishedSessionOptional = jooq.selectFrom(ENABLE_BANKING_SESSION)
                    .where(ENABLE_BANKING_SESSION.ID.eq(sessionId))
                    .and(ENABLE_BANKING_SESSION.BANK_ACCOUNT_IBAN.isNull().or(ENABLE_BANKING_SESSION.BANK_ACCOUNT_UID.isNull()).or(ENABLE_BANKING_SESSION.ACCOUNT_ID.isNull()))
                    .fetchOptional();
            if (unfinishedSessionOptional.isEmpty()) {
                stuff.set(Optional.empty());
                return;
            }
            var unfinishedSession = unfinishedSessionOptional.get();
            var accounts = jooq.select(
                            ENABLE_BANKING_SESSION_ACCOUNT.ID,
                            ENABLE_BANKING_SESSION_ACCOUNT.ACCOUNT_UID,
                            ENABLE_BANKING_SESSION_ACCOUNT.ACCOUNT_IBAN
                    ).from(ENABLE_BANKING_SESSION_ACCOUNT)
                    .where(ENABLE_BANKING_SESSION_ACCOUNT.SESSION_ID.eq(sessionId))
                    .fetch(mapping(EnableBankingAccountDto::new));

            stuff.set(Optional.of(new EnableBankingUnfinishedSession(
                    unfinishedSession.getId(),
                    unfinishedSession.getValidUntil(),
                    unfinishedSession.getBankName(),
                    accounts
            )));
        });
        return stuff.get();
    }

    @Override
    public List<EnableBankingSession> getAllSessions() {
        return jooq.select(
                        ENABLE_BANKING_SESSION.ID,
                        ENABLE_BANKING_SESSION.VALID_UNTIL,
                        ENABLE_BANKING_SESSION.BANK_NAME,
                        ENABLE_BANKING_SESSION.BANK_ACCOUNT_IBAN,
                        ENABLE_BANKING_SESSION.BANK_ACCOUNT_UID,
                        ENABLE_BANKING_SESSION.ACCOUNT_ID,
                        ACCOUNT.NAME
                )
                .from(ENABLE_BANKING_SESSION)
                .leftJoin(ACCOUNT).on(ENABLE_BANKING_SESSION.ACCOUNT_ID.eq(ACCOUNT.ID))
                .orderBy(ENABLE_BANKING_SESSION.ID.desc())
                .fetch(mapping(EnableBankingSession::new));
    }

    @Override
    public Optional<EnableBankingSession> getSession(long id) {
        return jooq.select(
                        ENABLE_BANKING_SESSION.ID,
                        ENABLE_BANKING_SESSION.VALID_UNTIL,
                        ENABLE_BANKING_SESSION.BANK_NAME,
                        ENABLE_BANKING_SESSION.BANK_ACCOUNT_IBAN,
                        ENABLE_BANKING_SESSION.BANK_ACCOUNT_UID,
                        ENABLE_BANKING_SESSION.ACCOUNT_ID,
                        ACCOUNT.NAME
                )
                .from(ENABLE_BANKING_SESSION)
                .leftJoin(ACCOUNT).on(ENABLE_BANKING_SESSION.ACCOUNT_ID.eq(ACCOUNT.ID))
                .where(ENABLE_BANKING_SESSION.ID.eq(id))
                .fetchOptional(mapping(EnableBankingSession::new));
    }

    @Override
    public void deleteSession(long sessionId) throws EnableBankingClientException {
        String enableBankingSessionId = jooq.select(
                        ENABLE_BANKING_SESSION.SESSION_ID
                ).from(ENABLE_BANKING_SESSION)
                .where(ENABLE_BANKING_SESSION.ID.eq(sessionId))
                .fetchOne()
                .value1();
        var response = enableBankingClient.deleteSession(enableBankingSessionId);
        if (!response.message().toLowerCase(Locale.GERMANY).equals("ok")) {
            final String errorMessage = "An error occurred when calling EnableBanking delete session API. API returned the following message: %s".formatted(response.message());
            log.error(errorMessage);
            throw new EnableBankingClientException(errorMessage);
        }
        // API call was successful, we can now delete all our records
        jooq.transaction(tx -> {
            tx.dsl().deleteFrom(ENABLE_BANKING_SESSION_ACCOUNT).where(ENABLE_BANKING_SESSION_ACCOUNT.SESSION_ID.eq(sessionId)).execute();
            tx.dsl().deleteFrom(ENABLE_BANKING_SESSION).where(ENABLE_BANKING_SESSION.ID.eq(sessionId)).execute();
        });
    }

    @Override
    public EnableBankingSession finishUnfinishedSession(long sessionId, FinishSessionRequest request) {
        jooq.update(ENABLE_BANKING_SESSION)
                .set(ENABLE_BANKING_SESSION.BANK_ACCOUNT_UID, request.bankAccountUid())
                .set(ENABLE_BANKING_SESSION.BANK_ACCOUNT_IBAN, request.bankAccountIban())
                .set(ENABLE_BANKING_SESSION.ACCOUNT_ID, request.accountId())
                .where(ENABLE_BANKING_SESSION.ID.eq(sessionId))
                .execute();
        return getSession(sessionId).orElseThrow();
    }

    // Sync status field references (new columns not yet in generated jOOQ code)
    private static final Field<OffsetDateTime> LAST_SYNCED_AT = DSL.field("last_synced_at", OffsetDateTime.class);
    private static final Field<LocalDate> LAST_SYNCED_DATE = DSL.field("last_synced_date", LocalDate.class);
    private static final Field<String> LAST_SYNC_STATUS = DSL.field("last_sync_status", String.class);
    private static final Field<String> LAST_SYNC_ERROR_MESSAGE = DSL.field("last_sync_error_message", String.class);
    private static final Field<Long> API_BALANCE_CENTS = DSL.field("api_balance_cents", Long.class);
    private static final Field<String> API_BALANCE_CURRENCY = DSL.field("api_balance_currency", String.class);
    private static final Field<Long> COMPUTED_BALANCE_CENTS = DSL.field("computed_balance_cents", Long.class);
    private static final Field<Boolean> BALANCE_MATCH = DSL.field("balance_match", Boolean.class);
    private static final Field<Long> OPENING_BALANCE_CENTS = DSL.field("opening_balance_cents", Long.class);

    @Override
    public List<EnableBankingSession> getActiveSessions() {
        return jooq.select(
                        ENABLE_BANKING_SESSION.ID,
                        ENABLE_BANKING_SESSION.VALID_UNTIL,
                        ENABLE_BANKING_SESSION.BANK_NAME,
                        ENABLE_BANKING_SESSION.BANK_ACCOUNT_IBAN,
                        ENABLE_BANKING_SESSION.BANK_ACCOUNT_UID,
                        ENABLE_BANKING_SESSION.ACCOUNT_ID,
                        ACCOUNT.NAME
                )
                .from(ENABLE_BANKING_SESSION)
                .leftJoin(ACCOUNT).on(ENABLE_BANKING_SESSION.ACCOUNT_ID.eq(ACCOUNT.ID))
                .where(ENABLE_BANKING_SESSION.BANK_ACCOUNT_UID.isNotNull())
                .and(ENABLE_BANKING_SESSION.VALID_UNTIL.gt(OffsetDateTime.now()))
                .orderBy(ENABLE_BANKING_SESSION.ID.desc())
                .fetch(mapping(EnableBankingSession::new));
    }

    @Override
    public void updateSyncStatus(long sessionId, OffsetDateTime lastSyncedAt, LocalDate lastSyncedDate,
                                  String lastSyncStatus, String lastSyncErrorMessage,
                                  Long apiBalanceCents, String apiBalanceCurrency,
                                  Long computedBalanceCents, Boolean balanceMatch) {
        jooq.update(ENABLE_BANKING_SESSION)
                .set(LAST_SYNCED_AT, lastSyncedAt)
                .set(LAST_SYNCED_DATE, lastSyncedDate)
                .set(LAST_SYNC_STATUS, lastSyncStatus)
                .set(LAST_SYNC_ERROR_MESSAGE, lastSyncErrorMessage)
                .set(API_BALANCE_CENTS, apiBalanceCents)
                .set(API_BALANCE_CURRENCY, apiBalanceCurrency)
                .set(COMPUTED_BALANCE_CENTS, computedBalanceCents)
                .set(BALANCE_MATCH, balanceMatch)
                .where(ENABLE_BANKING_SESSION.ID.eq(sessionId))
                .execute();
    }

    @Override
    public void setOpeningBalance(long sessionId, long openingBalanceCents) {
        jooq.update(ENABLE_BANKING_SESSION)
                .set(OPENING_BALANCE_CENTS, openingBalanceCents)
                .where(ENABLE_BANKING_SESSION.ID.eq(sessionId))
                .execute();
    }

    @Override
    public Optional<Long> getOpeningBalance(long sessionId) {
        return jooq.select(OPENING_BALANCE_CENTS)
                .from(ENABLE_BANKING_SESSION)
                .where(ENABLE_BANKING_SESSION.ID.eq(sessionId))
                .fetchOptional(OPENING_BALANCE_CENTS);
    }

    @Override
    public List<SessionSyncStatus> getAllSyncStatuses() {
        return jooq.select(
                        ENABLE_BANKING_SESSION.ID,
                        LAST_SYNCED_AT, LAST_SYNCED_DATE, LAST_SYNC_STATUS, LAST_SYNC_ERROR_MESSAGE,
                        API_BALANCE_CENTS, API_BALANCE_CURRENCY, COMPUTED_BALANCE_CENTS, BALANCE_MATCH
                )
                .from(ENABLE_BANKING_SESSION)
                .where(ENABLE_BANKING_SESSION.BANK_ACCOUNT_UID.isNotNull())
                .orderBy(ENABLE_BANKING_SESSION.ID.desc())
                .fetch(r -> new SessionSyncStatus(
                        r.get(ENABLE_BANKING_SESSION.ID),
                        r.get(LAST_SYNCED_AT),
                        r.get(LAST_SYNCED_DATE),
                        r.get(LAST_SYNC_STATUS),
                        r.get(LAST_SYNC_ERROR_MESSAGE),
                        r.get(API_BALANCE_CENTS),
                        r.get(API_BALANCE_CURRENCY),
                        r.get(COMPUTED_BALANCE_CENTS),
                        r.get(BALANCE_MATCH)
                ));
    }

    @Override
    public Optional<SessionSyncStatus> getSyncStatus(long sessionId) {
        return jooq.select(
                        ENABLE_BANKING_SESSION.ID,
                        LAST_SYNCED_AT, LAST_SYNCED_DATE, LAST_SYNC_STATUS, LAST_SYNC_ERROR_MESSAGE,
                        API_BALANCE_CENTS, API_BALANCE_CURRENCY, COMPUTED_BALANCE_CENTS, BALANCE_MATCH
                )
                .from(ENABLE_BANKING_SESSION)
                .where(ENABLE_BANKING_SESSION.ID.eq(sessionId))
                .fetchOptional(r -> new SessionSyncStatus(
                        r.get(ENABLE_BANKING_SESSION.ID),
                        r.get(LAST_SYNCED_AT),
                        r.get(LAST_SYNCED_DATE),
                        r.get(LAST_SYNC_STATUS),
                        r.get(LAST_SYNC_ERROR_MESSAGE),
                        r.get(API_BALANCE_CENTS),
                        r.get(API_BALANCE_CURRENCY),
                        r.get(COMPUTED_BALANCE_CENTS),
                        r.get(BALANCE_MATCH)
                ));
    }
}
