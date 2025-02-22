package de.flur4.roomiefunds.domain.api.person;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.person.CreatePersonDto;
import de.flur4.roomiefunds.models.person.Person;

public interface CreatePerson {
    Person createPerson(ModifyingPersonDto modifyingPerson, CreatePersonDto dto) throws JsonProcessingException;
}
