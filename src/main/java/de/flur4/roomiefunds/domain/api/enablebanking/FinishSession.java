package de.flur4.roomiefunds.domain.api.enablebanking;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.enablebanking.EnableBankingSession;
import de.flur4.roomiefunds.models.enablebanking.FinishSessionRequest;

public interface FinishSession {
    EnableBankingSession finishUnfinishedSession(ModifyingPersonDto modifyingPerson, long sessionId, FinishSessionRequest request) throws SessionNotFoundException, JsonProcessingException, SessionAlreadyFinishedException;
}
