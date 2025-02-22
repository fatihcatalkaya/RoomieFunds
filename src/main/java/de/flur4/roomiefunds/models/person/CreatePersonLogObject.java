package de.flur4.roomiefunds.models.person;

import de.flur4.roomiefunds.models.account.Account;

/**
 * This class is only used for correct logging of a newly created person
 * at its associated account.
 */
public record CreatePersonLogObject(Person person, Account account) {
}
