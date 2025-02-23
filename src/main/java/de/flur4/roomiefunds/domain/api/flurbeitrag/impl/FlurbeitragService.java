package de.flur4.roomiefunds.domain.api.flurbeitrag.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.flurbeitrag.CreateFlurbeitragTransaction;
import de.flur4.roomiefunds.domain.api.flurbeitrag.GetFlurbeitrag;
import de.flur4.roomiefunds.domain.api.flurbeitrag.SetFlurbeitrag;
import de.flur4.roomiefunds.domain.spi.FlurbeitragRepository;
import de.flur4.roomiefunds.domain.spi.FlurkontoRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.PersonRepository;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.flurbeitrag.FlurbeitragSchedulerExitCodes;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@RequiredArgsConstructor
public class FlurbeitragService implements GetFlurbeitrag, SetFlurbeitrag, CreateFlurbeitragTransaction {
    final static String SETTINGS_FLURBEITRAG_AMOUNT = "settings.flurbeitrag_amount";
    final static ModifyingPersonDto SCHEDULER = new ModifyingPersonDto("", Optional.of("Flurbeitrag Scheduler"));
    final FlurbeitragRepository flurbeitragRepository;
    final FlurkontoRepository flurkontoRepository;
    final PersonRepository personRepository;
    final LogRepository logRepository;

    @Override
    public long getFlurbeitrag() {
        return flurbeitragRepository.getFlurbeitrag();
    }

    @Override
    public void setFlurbeitrag(ModifyingPersonDto modifyingPerson, long flurbeitrag) throws JsonProcessingException {
        long flurbeitragBefore = flurbeitragRepository.getFlurbeitrag();
        flurbeitragRepository.setFlurbeitrag(flurbeitrag);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.update,
                SETTINGS_FLURBEITRAG_AMOUNT,
                Optional.of(flurbeitragBefore),
                Optional.of(flurbeitrag)
        ));
    }

    @Override
    public FlurbeitragSchedulerExitCodes createFlurbeitragTransaction() {
        final LocalDate today = LocalDate.now(ZoneId.systemDefault());
        // first, we have to check if the transaction has already been created for this month
        if(flurbeitragRepository.existsFlurbeitragForMonthInYear(today.getMonthValue(), today.getYear())) {
            // We have already created the transaction for this month => Skip
            return FlurbeitragSchedulerExitCodes.ALREADY_CREATED_IN_CURRENT_MONTH;
        }
        // Check if Flurkonto has already been configured
        var flurkonto = flurkontoRepository.getFlurkonto();
        if(flurkonto.isEmpty()){
            // Flurkonto is not configured. We do not know to which account to book the
            // Flurbeitrag to. Therefore, we skip for now
            return FlurbeitragSchedulerExitCodes.FLURKONTO_NOT_CONFIGURED;
        }
        // Load the flurbeitrag
        final long flurbeitrag = flurbeitragRepository.getFlurbeitrag();
        if(flurbeitrag == 0) {
            return FlurbeitragSchedulerExitCodes.FLURBEITRAG_EQUALS_ZERO;
        }
        // We load the list of persons from which we have to take money
        final var persons = personRepository.getPersonsThatPayFlurbeitrag();
        flurbeitragRepository.createFlurbeitragTransactions(SCHEDULER, persons, flurbeitrag, flurkonto.get().id(), today);
        return FlurbeitragSchedulerExitCodes.OK;
    }
}
