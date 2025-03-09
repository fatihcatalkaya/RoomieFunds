package de.flur4.roomiefunds.domain.api.enablebanking.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.enablebanking.*;
import de.flur4.roomiefunds.domain.spi.EnableBankingRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.infrastructure.webclient.enablebanking.EnableBankingClient;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingSession;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingUnfinishedSession;
import de.flur4.roomiefunds.models.enablebanking.FinishSessionRequest;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import de.flur4.roomiefunds.models.webclient.enablebanking.HalTransactions;
import de.flur4.roomiefunds.models.webclient.enablebanking.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.jooq.tools.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@JBossLog
@RequiredArgsConstructor
public class EnableBankingService implements GetSession, FinishSession, DeleteSession, SynchronizeTransactions {
    final EnableBankingClient enableBankingClient;
    final EnableBankingRepository enableBankingRepository;
    final LogRepository logRepository;

    @Override
    public Optional<EnableBankingUnfinishedSession> getUnfinishedSession(long sessionId) {
        return enableBankingRepository.getUnfinishedSession(sessionId);
    }

    @Override
    public List<EnableBankingSession> getAllSessions() {
        return enableBankingRepository.getAllSessions();
    }

    @Override
    public EnableBankingSession finishUnfinishedSession(ModifyingPersonDto modifyingPerson, long sessionId, FinishSessionRequest request) throws SessionNotFoundException, JsonProcessingException, SessionAlreadyFinishedException {
        var session = enableBankingRepository.getSession(sessionId);
        if (session.isEmpty()) {
            throw new SessionNotFoundException(sessionId);
        }
        var sessionBefore = session.get();
        if(!StringUtils.isEmpty(sessionBefore.bankAccountUid()) && !StringUtils.isEmpty(sessionBefore.bankAccountIban()) && sessionBefore.accountId() != null ) {
            throw new SessionAlreadyFinishedException(sessionId);
        }

        var sessionAfter = enableBankingRepository.finishUnfinishedSession(sessionId, request);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.update,
                "session",
                Optional.of(sessionBefore),
                Optional.of(sessionAfter)
        ));
        return sessionAfter;
    }

    @Override
    public void deleteSession(ModifyingPersonDto modifyingPerson, long sessionId) throws SessionNotFoundException, EnableBankingClientException, JsonProcessingException {
        var session = enableBankingRepository.getSession(sessionId);
        if (session.isEmpty()) {
            throw new SessionNotFoundException(sessionId);
        }
        enableBankingRepository.deleteSession(sessionId);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.delete,
                "session",
                Optional.of(session),
                Optional.empty()
        ));
    }

    @Override
    public void synchronizeTransactions() {
        var activeSessions = enableBankingRepository.getActiveFinishedSessions();
        for(var session : activeSessions) {
            List<Transaction> fetchedTransactions = new ArrayList<>();
            try{
                HalTransactions result;
                String continuationKey = null;
                do {
                    result = enableBankingClient.getAccountTransactions(
                            session.bankAccountUid(),
                            null, //LocalDate.now(Clock.systemUTC()).minusDays(7),
                            null, //LocalDate.now(Clock.systemUTC()),
                            continuationKey,
                            null,
                            null
                    );
                    fetchedTransactions.addAll(result.transactions());
                    continuationKey = result.continuationKey();
                } while(continuationKey != null && !continuationKey.isEmpty());
            } catch(Exception ex) {
                log.error("An exception occurred when querying transactions for session %s", session.toString(), ex);
            }
        }
    }
}
