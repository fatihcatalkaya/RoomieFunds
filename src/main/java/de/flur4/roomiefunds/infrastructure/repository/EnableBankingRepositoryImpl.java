package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.api.enablebanking.EnableBankingClientException;
import de.flur4.roomiefunds.domain.spi.EnableBankingRepository;
import de.flur4.roomiefunds.infrastructure.webclient.enablebanking.EnableBankingClient;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingAccountDto;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingSession;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingUnfinishedSession;
import de.flur4.roomiefunds.models.enablebanking.FinishSessionRequest;
import de.flur4.roomiefunds.models.webclient.enablebanking.AuthorizeSessionResponse;
import de.flur4.roomiefunds.models.webclient.enablebanking.CashAccountType;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jooq.DSLContext;
import org.jooq.tools.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.ENABLE_BANKING_SESSION;
import static de.flur4.roomiefunds.infrastructure.jooq.Tables.ENABLE_BANKING_SESSION_ACCOUNT;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.currentOffsetDateTime;

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
                String accountIban = accountResource.accountId().iban();
                if (StringUtils.isEmpty(accountIban)) {
                    accountIban = accountResource.accountId().other().identification();
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
                    .where(ENABLE_BANKING_SESSION_ACCOUNT.ID.eq(sessionId))
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
                        ENABLE_BANKING_SESSION.ACCOUNT_ID
                )
                .from(ENABLE_BANKING_SESSION)
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
                        ENABLE_BANKING_SESSION.ACCOUNT_ID
                )
                .from(ENABLE_BANKING_SESSION)
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
        return jooq.update(ENABLE_BANKING_SESSION)
                .set(ENABLE_BANKING_SESSION.BANK_ACCOUNT_UID, request.bankAccountUid())
                .set(ENABLE_BANKING_SESSION.BANK_ACCOUNT_IBAN, request.bankAccountIban())
                .set(ENABLE_BANKING_SESSION.ACCOUNT_ID, request.accountId())
                .where(ENABLE_BANKING_SESSION.ID.eq(sessionId))
                .returningResult(
                        ENABLE_BANKING_SESSION.ID,
                        ENABLE_BANKING_SESSION.VALID_UNTIL,
                        ENABLE_BANKING_SESSION.BANK_NAME,
                        ENABLE_BANKING_SESSION.BANK_ACCOUNT_IBAN,
                        ENABLE_BANKING_SESSION.BANK_ACCOUNT_UID,
                        ENABLE_BANKING_SESSION.ACCOUNT_ID
                ).fetchOne(mapping(EnableBankingSession::new));
    }

    @Override
    public List<EnableBankingSession> getActiveFinishedSessions() {
        return jooq.select(
                        ENABLE_BANKING_SESSION.ID,
                        ENABLE_BANKING_SESSION.VALID_UNTIL,
                        ENABLE_BANKING_SESSION.BANK_NAME,
                        ENABLE_BANKING_SESSION.BANK_ACCOUNT_IBAN,
                        ENABLE_BANKING_SESSION.BANK_ACCOUNT_UID,
                        ENABLE_BANKING_SESSION.ACCOUNT_ID
                ).from(ENABLE_BANKING_SESSION)
                .where(ENABLE_BANKING_SESSION.VALID_UNTIL.gt(currentOffsetDateTime()))
                .and(ENABLE_BANKING_SESSION.BANK_ACCOUNT_IBAN.isNotNull())
                .and(ENABLE_BANKING_SESSION.BANK_ACCOUNT_UID.isNotNull())
                .and(ENABLE_BANKING_SESSION.ACCOUNT_ID.isNotNull())
                .fetch(mapping(EnableBankingSession::new));
    }
}
