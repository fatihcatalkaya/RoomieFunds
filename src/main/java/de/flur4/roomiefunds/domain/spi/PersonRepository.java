package de.flur4.roomiefunds.domain.spi;


import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.person.CreatePersonDto;
import de.flur4.roomiefunds.models.person.Person;
import de.flur4.roomiefunds.models.person.UpdatePersonDto;
import org.javatuples.Pair;
import org.jooq.exception.DataAccessException;

import java.util.List;
import java.util.Optional;

public interface PersonRepository {
    Optional<Person> getPersonById(long id);

    List<Person> getAllPersons();

    List<Person> getPersonsToPrintOnTallyList();

    Pair<Person, Account> createPerson(CreatePersonDto createPersonDto);

    Person updatePerson(long personId, UpdatePersonDto updatePersonDto);

    void deletePerson(long personId) throws DataAccessException;
}
