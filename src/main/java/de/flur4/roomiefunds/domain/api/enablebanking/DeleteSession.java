package de.flur4.roomiefunds.domain.api.enablebanking;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;

public interface DeleteSession {
    void deleteSession(ModifyingPersonDto modifyingPersonDto, long sessionId) throws SessionNotFoundException, EnableBankingClientException, JsonProcessingException;
}
