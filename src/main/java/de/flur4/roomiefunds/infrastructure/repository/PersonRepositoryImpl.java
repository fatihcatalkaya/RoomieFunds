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
import static org.jooq.impl.DSL.length;

@ApplicationScoped
@RequiredArgsConstructor
public class PersonRepositoryImpl implements PersonRepository {

    final DSLContext jooq;
    private static final String DEFAULT_PERSON_ACCOUNT_NAME = "Passiv:Bewohner:%s %s %s";

    @Override
    public Optional<Person> getPersonById(long id) {
        return jooq.select(
                        PERSON.ID,
                        PERSON.FIRST_NAME,
                        PERSON.LAST_NAME,
                        PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES,
                        PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST,
                        PERSON.EMAIL,
                        PERSON.EMAIL_ACCOUNT_STATEMENT,
                        PERSON.KEYCLOAK_USER_ID
                ).from(PERSON)
                .where(PERSON.ID.eq(id))
                .orderBy(PERSON.ID)
                .fetchOptional(mapping(Person::new));
    }

    @Override
    public List<Person> getAllPersons() {
        return jooq.select(
                        PERSON.ID,
                        PERSON.FIRST_NAME,
                        PERSON.LAST_NAME,
                        PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES,
                        PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST,
                        PERSON.EMAIL,
                        PERSON.EMAIL_ACCOUNT_STATEMENT,
                        PERSON.KEYCLOAK_USER_ID
                ).from(PERSON)
                .orderBy(PERSON.FIRST_NAME)
                .fetch(mapping(Person::new));
    }

    @Override
    public List<Person> getPersonsToPrintOnTallyList() {
        return jooq.select(
                        PERSON.ID,
                        PERSON.FIRST_NAME,
                        PERSON.LAST_NAME,
                        PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES,
                        PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST,
                        PERSON.EMAIL,
                        PERSON.EMAIL_ACCOUNT_STATEMENT,
                        PERSON.KEYCLOAK_USER_ID
                ).from(PERSON)
                .where(PERSON.PRINT_ON_PRODUCT_TALLY_LIST.eq(true))
                .orderBy(PERSON.ROOM)
                .fetch(mapping(Person::new));
    }

    @Override
    public List<Person> getPersonsThatPayFlurbeitrag() {
        return jooq.select(
                        PERSON.ID,
                        PERSON.FIRST_NAME,
                        PERSON.LAST_NAME,
                        PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES,
                        PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST,
                        PERSON.EMAIL,
                        PERSON.EMAIL_ACCOUNT_STATEMENT,
                        PERSON.KEYCLOAK_USER_ID
                ).from(PERSON)
                .where(PERSON.PAYS_FLOOR_FEES.eq(true))
                .orderBy(PERSON.ROOM)
                .fetch(mapping(Person::new));
    }

    @Override
    public Pair<Person, Account> createPerson(CreatePersonDto createPersonDto) {
        final String accountName = DEFAULT_PERSON_ACCOUNT_NAME.formatted(createPersonDto.firstName(), createPersonDto.lastName(), createPersonDto.room());
        AtomicReference<Account> account = new AtomicReference<>();
        AtomicReference<Person> person = new AtomicReference<>();
        // Since we have to create both, Account and Person, we are going to use a DB transaction for that
        jooq.transaction(tx -> {
            account.set(tx.dsl().insertInto(ACCOUNT)
                    .columns(ACCOUNT.NAME, ACCOUNT.IS_ACTIVE)
                    .values(accountName, false)
                    .returningResult(ACCOUNT.ID, ACCOUNT.NAME, ACCOUNT.IS_ACTIVE)
                    .fetchOne(mapping(Account::new)));
            person.set(tx.dsl().insertInto(PERSON)
                    .columns(
                            PERSON.FIRST_NAME,
                            PERSON.LAST_NAME,
                            PERSON.ROOM,
                            PERSON.PAYS_FLOOR_FEES,
                            PERSON.ACCOUNT_ID,
                            PERSON.PRINT_ON_PRODUCT_TALLY_LIST,
                            PERSON.EMAIL,
                            PERSON.EMAIL_ACCOUNT_STATEMENT
                    ).values(
                            createPersonDto.firstName(),
                            createPersonDto.lastName(),
                            createPersonDto.room(),
                            createPersonDto.paysFloorFees(),
                            account.get().id(),
                            createPersonDto.printOnProductTallyList(),
                            createPersonDto.email().orElse(""),
                            createPersonDto.emailAccountStatement()
                    ).returningResult(
                            PERSON.ID,
                            PERSON.FIRST_NAME,
                            PERSON.LAST_NAME,
                            PERSON.ROOM,
                            PERSON.PAYS_FLOOR_FEES,
                            PERSON.ACCOUNT_ID,
                            PERSON.PRINT_ON_PRODUCT_TALLY_LIST,
                            PERSON.EMAIL,
                            PERSON.EMAIL_ACCOUNT_STATEMENT,
                            PERSON.KEYCLOAK_USER_ID
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
        if (updatePersonDto.firstName().isPresent()) {
            person.setFirstName(updatePersonDto.firstName().get());
        }
        if (updatePersonDto.lastName().isPresent()) {
            person.setLastName(updatePersonDto.lastName().get());
        }
        if (updatePersonDto.room().isPresent()) {
            person.setRoom(updatePersonDto.room().get());
        }
        if (updatePersonDto.paysFloorFees().isPresent()) {
            person.setPaysFloorFees(updatePersonDto.paysFloorFees().get());
        }
        if (updatePersonDto.printOnProductTallyList().isPresent()) {
            person.setPrintOnProductTallyList(updatePersonDto.printOnProductTallyList().get());
        }
        if (updatePersonDto.email().isPresent()) {
            person.setEmail(updatePersonDto.email().get());
        }
        if (updatePersonDto.emailAccountStatement().isPresent()) {
            person.setEmailAccountStatement(updatePersonDto.emailAccountStatement().get());
        }
        person.store();
        return new Person(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getRoom(),
                person.getPaysFloorFees(),
                person.getAccountId(),
                person.getPrintOnProductTallyList(),
                person.getEmail(),
                person.getEmailAccountStatement(),
                person.getKeycloakUserId()
        );
    }

    @Override
    public void deletePerson(long personId) throws DataAccessException {
        var person = jooq.select(
                        PERSON.ID,
                        PERSON.FIRST_NAME,
                        PERSON.LAST_NAME,
                        PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES,
                        PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST,
                        PERSON.EMAIL,
                        PERSON.EMAIL_ACCOUNT_STATEMENT,
                        PERSON.KEYCLOAK_USER_ID
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

    @Override
    public void updatePersonKeycloakUserId(long personId, String keycloakUserId) {
        jooq.update(PERSON)
                .set(PERSON.KEYCLOAK_USER_ID, keycloakUserId)
                .where(PERSON.ID.eq(personId))
                .execute();
    }

    @Override
    public List<Person> getPersonsWithValidEmails() {
        return jooq.select(
                        PERSON.ID,
                        PERSON.FIRST_NAME,
                        PERSON.LAST_NAME,
                        PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES,
                        PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST,
                        PERSON.EMAIL,
                        PERSON.EMAIL_ACCOUNT_STATEMENT,
                        PERSON.KEYCLOAK_USER_ID
                ).from(PERSON)
                .where(PERSON.EMAIL_ACCOUNT_STATEMENT.eq(true))
                .and(length(PERSON.EMAIL).ge(0))
                .fetch(mapping(Person::new));
    }
}
