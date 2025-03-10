package de.flur4.roomiefunds.models.account;

import de.flur4.roomiefunds.models.person.Person;
import org.javatuples.Pair;

import java.util.List;

public record SendAccountStatementsResult(
        List<Person> successfulSendPersons,
        List<Pair<Person, Exception>> failedPersons
) {
}
