package de.flur4.roomiefunds.domain.api.person;

public class PersonUndeletableException extends Exception {
    public PersonUndeletableException(long personId, String personName) {
        super("Person '%s' (%d) can not be deleted, because there are already associated transactions.".formatted(personName, personId));
    }
}
