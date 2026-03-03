package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.domain.api.enablebanking.EnableBankingClientException;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingSession;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingUnfinishedSession;
import de.flur4.roomiefunds.models.enablebanking.FinishSessionRequest;
import de.flur4.roomiefunds.models.enablebanking.SessionSyncStatus;
import de.flur4.roomiefunds.models.webclient.enablebanking.AuthorizeSessionResponse;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface EnableBankingRepository {
    Optional<EnableBankingUnfinishedSession> getUnfinishedSession(long sessionId);
    List<EnableBankingSession> getAllSessions();
    Optional<EnableBankingSession> getSession(long id);

    void deleteSession(long sessionId) throws EnableBankingClientException;

    EnableBankingSession finishUnfinishedSession(long sessionId, FinishSessionRequest request);

    void storeNewSession(AuthorizeSessionResponse dto);

    List<EnableBankingSession> getActiveSessions();

    void updateSyncStatus(long sessionId, OffsetDateTime lastSyncedAt, LocalDate lastSyncedDate,
                          String lastSyncStatus, String lastSyncErrorMessage,
                          Long apiBalanceCents, String apiBalanceCurrency,
                          Long computedBalanceCents, Boolean balanceMatch);

    void setOpeningBalance(long sessionId, long openingBalanceCents);

    Optional<Long> getOpeningBalance(long sessionId);

    List<SessionSyncStatus> getAllSyncStatuses();

    Optional<SessionSyncStatus> getSyncStatus(long sessionId);
}
