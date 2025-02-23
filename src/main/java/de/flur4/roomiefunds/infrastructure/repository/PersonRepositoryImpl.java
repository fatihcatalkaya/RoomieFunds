package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.PersonRepository;
import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.person.CreatePersonDto;
import de.flur4.roomiefunds.models.person.Person;
import de.flur4.roomiefunds.models.person.UpdatePersonDto;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.javatuples.Pair;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.*;
import static org.jooq.Records.mapping;

@ApplicationScoped
@RequiredArgsConstructor
public class PersonRepositoryImpl implements PersonRepository {

    final DSLContext jooq;
    private static final String DEFAULT_PERSON_ACCOUNT_NAME = "Passiv:Bewohner:%s %s";

    @Override
    public Optional<Person> getPersonById(long id) {
        return jooq.select(
                        PERSON.ID,
                        PERSON.NAME,
                        PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES,
                        PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST
                ).from(PERSON)
                .where(PERSON.ID.eq(id))
                .orderBy(PERSON.ID)
                .fetchOptional(mapping(Person::new));
    }

    @Override
    public List<Person> getAllPersons() {
        return jooq.select(
                        PERSON.ID,
                        PERSON.NAME,
                        PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES,
                        PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST
                ).from(PERSON)
                .orderBy(PERSON.NAME)
                .fetch(mapping(Person::new));
    }

    @Override
    public List<Person> getPersonsToPrintOnTallyList() {
        return jooq.select(
                        PERSON.ID,
                        PERSON.NAME,
                        PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES,
                        PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST
                ).from(PERSON)
                .where(PERSON.PRINT_ON_PRODUCT_TALLY_LIST.eq(true))
                .orderBy(PERSON.ROOM)
                .fetch(mapping(Person::new));
    }

    @Override
    public List<Person> getPersonsThatPayFlurbeitrag() {
        return jooq.select(
                        PERSON.ID,
                        PERSON.NAME,
                        PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES,
                        PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST
                ).from(PERSON)
                .where(PERSON.PAYS_FLOOR_FEES.eq(true))
                .orderBy(PERSON.ROOM)
                .fetch(mapping(Person::new));
    }

    @Override
    public Pair<Person, Account> createPerson(CreatePersonDto createPersonDto) {
        final String accountName = DEFAULT_PERSON_ACCOUNT_NAME.formatted(createPersonDto.name(), createPersonDto.room());
        AtomicReference<Account> account = new AtomicReference<>();
        AtomicReference<Person> person = new AtomicReference<>();
        // Since we have to create both, Account and Person, we are going to use a DB transaction for that
        jooq.transaction(tx -> {
            account.set(tx.dsl().insertInto(ACCOUNT)
                    .columns(ACCOUNT.NAME, ACCOUNT.IS_ACTIVE)
                    .values(accountName, false)
                    .returningResult(ACCOUNT.ID, ACCOUNT.NAME, ACCOUNT.IS_ACTIVE)
                    .fetchOne(mapping(Account::new)));
            person.set(jooq.insertInto(PERSON)
                    .columns(
                            PERSON.NAME,
                            PERSON.ROOM,
                            PERSON.PAYS_FLOOR_FEES,
                            PERSON.ACCOUNT_ID,
                            PERSON.PRINT_ON_PRODUCT_TALLY_LIST
                    ).values(
                            createPersonDto.name(),
                            createPersonDto.room(),
                            createPersonDto.paysFloorFees(),
                            account.get().id(),
                            createPersonDto.printOnProductTallyList()
                    ).returningResult(
                            PERSON.ID,
                            PERSON.NAME,
                            PERSON.ROOM,
                            PERSON.PAYS_FLOOR_FEES,
                            PERSON.ACCOUNT_ID,
                            PERSON.PRINT_ON_PRODUCT_TALLY_LIST
                    ).fetchOne(mapping(Person::new)));
        });
        return new Pair<>(person.get(), account.get());
    }

    @Override
    public Person updatePerson(long personId, UpdatePersonDto updatePersonDto) {
        var person = jooq.selectFrom(PERSON)
                .where(PERSON.ID.eq(personId))
                .fetchOne();
        assert person != null;
        if (updatePersonDto.name().isPresent()) {
            person.setName(updatePersonDto.name().get());
        }
        if (updatePersonDto.room().isPresent()) {
            person.setRoom(updatePersonDto.room().get());
        }
        if (updatePersonDto.paysFloorFees().isPresent()) {
            person.setPaysFloorFees(updatePersonDto.paysFloorFees().get());
        }
        if(updatePersonDto.printOnProductTallyList().isPresent()) {
            person.setPrintOnProductTallyList(updatePersonDto.printOnProductTallyList().get());
        }
        person.store();
        return new Person(
                person.getId(),
                person.getName(),
                person.getRoom(),
                person.getPaysFloorFees(),
                person.getAccountId(),
                person.getPrintOnProductTallyList()
        );
    }

    @Override
    public void deletePerson(long personId) throws DataAccessException {
        var person = jooq.select(
                        PERSON.ID,
                        PERSON.NAME,
                        PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES,
                        PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST
                ).from(PERSON)
                .where(PERSON.ID.eq(personId))
                .fetchOne(mapping(Person::new));
        // Since we have to delete both, Account and Person, we are going to use a DB transaction for that
        jooq.transaction(tx -> {
            // First delete the person, then the account so that the foreign key constraint does not break
            jooq.deleteFrom(PERSON).where(PERSON.ID.eq(personId)).execute();
            jooq.deleteFrom(ACCOUNT).where(ACCOUNT.ID.eq(person.accountId())).execute();
        });
    }
}
