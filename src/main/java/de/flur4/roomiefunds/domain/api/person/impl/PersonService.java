package de.flur4.roomiefunds.domain.api.person.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.person.*;
import de.flur4.roomiefunds.domain.spi.AccountRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.PersonRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import de.flur4.roomiefunds.models.person.*;
import lombok.RequiredArgsConstructor;
import org.jooq.exception.DataAccessException;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class PersonService implements CreatePerson, GetPerson, UpdatePerson, DeletePerson {
    final PersonRepository personRepository;
    final AccountRepository accountRepository;
    final TransactionRepository transactionRepository;
    final LogRepository logRepository;

    @Override
    public Person createPerson(ModifyingPersonDto modifyingPerson, CreatePersonDto dto) throws JsonProcessingException {
        var result = personRepository.createPerson(dto);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.create,
                "person,account",
                Optional.empty(),
                Optional.of(new CreatePersonLogObject(result.getValue0(), result.getValue1()))
        ));
        return result.getValue0();
    }

    @Override
    public void deletePerson(ModifyingPersonDto modifyingPerson, long personId) throws PersonUndeletableException, PersonNotFoundException, JsonProcessingException, DataAccessException {
        var personResult = personRepository.getPersonById(personId);
        if (personResult.isEmpty()) {
            throw new PersonNotFoundException(personId);
        }
        var person = personResult.get();
        if (transactionRepository.accountHasTransactions(person.accountId())) {
            throw new PersonUndeletableException(person.id(), person.name());
        }
        var account = accountRepository.getAccount(person.accountId()).get();
        personRepository.deletePerson(personId);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.delete,
                "person,account",
                Optional.of(new DeletePersonLogObject(person, account)),
                Optional.empty()
        ));
    }

    @Override
    public Optional<Person> getPerson(long personId) {
        return personRepository.getPersonById(personId);
    }

    @Override
    public List<Person> getPersons() {
        return personRepository.getAllPersons();
    }

    @Override
    public Person updatePerson(ModifyingPersonDto modifyingPerson, long personId, UpdatePersonDto updatePersonDto) throws PersonNotFoundException, JsonProcessingException {
        var person = personRepository.getPersonById(personId);
        if (person.isEmpty()) {
            throw new PersonNotFoundException(personId);
        }
        Person updatedPerson = personRepository.updatePerson(personId, updatePersonDto);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.update,
                "product",
                Optional.of(person),
                Optional.of(updatedPerson)
        ));
        return updatedPerson;
    }
}
