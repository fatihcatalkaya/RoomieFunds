package de.flur4.roomiefunds.domain.api.flurbeitrag;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;

public interface SetFlurbeitrag {
    void setFlurbeitrag(ModifyingPersonDto modifyingPerson, long flurbeitrag) throws JsonProcessingException;
}
