package de.flur4.roomiefunds.domain.api.person;

public class PersonNotFoundException extends Exception {
    public PersonNotFoundException(long personId) {
        super("Could not find person with id %d".formatted(personId));
    }
}
