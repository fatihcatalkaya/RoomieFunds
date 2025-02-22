package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.person.CreatePerson;
import de.flur4.roomiefunds.domain.api.person.DeletePerson;
import de.flur4.roomiefunds.domain.api.person.GetPerson;
import de.flur4.roomiefunds.domain.api.person.UpdatePerson;
import de.flur4.roomiefunds.domain.api.person.impl.PersonService;
import de.flur4.roomiefunds.domain.spi.AccountRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.domain.spi.PersonRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class PersonContext {
    @Produces
    @ApplicationScoped
    public CreatePerson createPerson(PersonRepository personRepository,
                                     AccountRepository accountRepository,
                                     TransactionRepository transactionRepository,
                                     LogRepository logRepository) {
        return new PersonService(personRepository, accountRepository, transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public UpdatePerson updatePerson(PersonRepository personRepository,
                                     AccountRepository accountRepository,
                                     TransactionRepository transactionRepository,
                                     LogRepository logRepository) {
        return new PersonService(personRepository, accountRepository, transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public GetPerson getPerson(PersonRepository personRepository,
                               AccountRepository accountRepository,
                               TransactionRepository transactionRepository,
                               LogRepository logRepository) {
        return new PersonService(personRepository, accountRepository, transactionRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public DeletePerson deletePerson(PersonRepository personRepository,
                                     AccountRepository accountRepository,
                                     TransactionRepository transactionRepository,
                                     LogRepository logRepository) {
        return new PersonService(personRepository, accountRepository, transactionRepository, logRepository);
    }
}
