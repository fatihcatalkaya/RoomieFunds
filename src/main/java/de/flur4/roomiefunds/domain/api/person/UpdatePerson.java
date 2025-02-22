package de.flur4.roomiefunds.domain.api.person;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.person.Person;
import de.flur4.roomiefunds.models.person.UpdatePersonDto;

public interface UpdatePerson {
    Person updatePerson(ModifyingPersonDto modifyingPerson, long personId, UpdatePersonDto updatePersonDto) throws PersonNotFoundException, JsonProcessingException;
}
