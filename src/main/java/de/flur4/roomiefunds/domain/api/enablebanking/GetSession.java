package de.flur4.roomiefunds.domain.api.enablebanking;

import de.flur4.roomiefunds.models.enablebanking.EnableBankingSession;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingUnfinishedSession;


import java.util.List;
import java.util.Optional;

public interface GetSession {
    Optional<EnableBankingUnfinishedSession> getUnfinishedSession(long sessionId);
    List<EnableBankingSession> getAllSessions();
}
