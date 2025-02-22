package de.flur4.roomiefunds.domain.api.account;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.account.CreateAccountDto;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;

public interface CreateAccount {
    Account createAccount(ModifyingPersonDto modifyingPerson, CreateAccountDto createAccountDto) throws JsonProcessingException;
}
