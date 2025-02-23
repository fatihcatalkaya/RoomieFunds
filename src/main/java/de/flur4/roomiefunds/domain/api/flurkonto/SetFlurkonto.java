package de.flur4.roomiefunds.domain.api.flurkonto;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.account.AccountNotFoundException;
import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;

public interface SetFlurkonto {
    Account setFlurkontoId(ModifyingPersonDto modifyingPersonDto, long accountId) throws JsonProcessingException, AccountNotFoundException;
}
