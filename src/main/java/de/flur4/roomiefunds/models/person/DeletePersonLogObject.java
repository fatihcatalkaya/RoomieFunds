package de.flur4.roomiefunds.models.person;

import de.flur4.roomiefunds.models.account.Account;

/**
 * This class is only used for correct logging of the deletion
 * of a person at its associated account.
 */
public record DeletePersonLogObject(Person person, Account account) {
}
