package de.flur4.roomiefunds.domain.api.flurbeitrag.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.flurbeitrag.GetFlurbeitrag;
import de.flur4.roomiefunds.domain.api.flurbeitrag.SetFlurbeitrag;
import de.flur4.roomiefunds.domain.spi.FlurbeitragRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class FlurbeitragService implements GetFlurbeitrag, SetFlurbeitrag {
    final static String SETTINGS_FLURBEITRAG_AMOUNT = "settings.flurbeitrag_amount";
    final FlurbeitragRepository flurbeitragRepository;
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
}
