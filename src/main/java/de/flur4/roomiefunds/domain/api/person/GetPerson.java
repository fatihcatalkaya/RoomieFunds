package de.flur4.roomiefunds.domain.api.person;

import de.flur4.roomiefunds.models.person.Person;

import java.util.List;
import java.util.Optional;

public interface GetPerson {
    Optional<Person> getPerson(long personId);
    List<Person> getPersons();
}
