package de.flur4.roomiefunds.domain.api.account;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;

public interface DeleteAccount {
    void deleteAccount(ModifyingPersonDto modifyingPerson, long accountId) throws AccountNotFoundException, AccountUndeletableException, JsonProcessingException;
}
