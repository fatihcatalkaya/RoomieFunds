package de.flur4.roomiefunds.domain.api.account;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.account.UpdateAccountDto;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;

public interface UpdateAccount {
    Account updateAccount(ModifyingPersonDto modifyingPerson, long accountId, UpdateAccountDto updateAccountDto) throws AccountNotFoundException, JsonProcessingException;
}
