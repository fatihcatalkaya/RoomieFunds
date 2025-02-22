package de.flur4.roomiefunds.domain.api.person;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;

public interface DeletePerson {
    void deletePerson(ModifyingPersonDto modifyingPerson, long personId) throws PersonNotFoundException, JsonProcessingException, PersonUndeletableException;
}
