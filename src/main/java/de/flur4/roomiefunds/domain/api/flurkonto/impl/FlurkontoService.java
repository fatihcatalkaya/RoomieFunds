package de.flur4.roomiefunds.domain.api.flurkonto.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.account.AccountNotFoundException;
import de.flur4.roomiefunds.domain.api.flurkonto.GetFlurkonto;
import de.flur4.roomiefunds.domain.api.flurkonto.SetFlurkonto;
import de.flur4.roomiefunds.domain.spi.AccountRepository;
import de.flur4.roomiefunds.domain.spi.FlurkontoRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class FlurkontoService implements GetFlurkonto, SetFlurkonto {

    final static String SETTINGS_FLUR_ACCOUNT_ID = "settings.flur_account_id";
    final FlurkontoRepository flurkontoRepository;
    final AccountRepository accountRepository;
    final LogRepository logRepository;

    @Override
    public Optional<Account> getFlurkonto() {
        return flurkontoRepository.getFlurkonto();
    }

    @Override
    public Account setFlurkontoId(ModifyingPersonDto modifyingPerson, long accountId) throws JsonProcessingException, AccountNotFoundException {
        if(accountRepository.getAccount(accountId).isEmpty()){
            throw new AccountNotFoundException(accountId);
        }

        var flurkontoBefore = flurkontoRepository.getFlurkonto();
        var flurkontoAfter = flurkontoRepository.setFlurkonto(accountId);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.update,
                SETTINGS_FLUR_ACCOUNT_ID,
                flurkontoBefore.isPresent() ? Optional.of(flurkontoBefore) : Optional.empty(),
                Optional.of(flurkontoAfter)
        ));
        return flurkontoAfter;
    }
}
