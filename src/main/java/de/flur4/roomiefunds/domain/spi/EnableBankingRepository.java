package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.domain.api.enablebanking.EnableBankingClientException;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingSession;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingUnfinishedSession;
import de.flur4.roomiefunds.models.enablebanking.FinishSessionRequest;

import java.util.List;
import java.util.Optional;

public interface EnableBankingRepository {
    Optional<EnableBankingUnfinishedSession> getUnfinishedSession(long sessionId);
    List<EnableBankingSession> getAllSessions();
    Optional<EnableBankingSession> getSession(long id);

    void deleteSession(long sessionId) throws EnableBankingClientException;

    EnableBankingSession finishUnfinishedSession(long sessionId, FinishSessionRequest request);
}
