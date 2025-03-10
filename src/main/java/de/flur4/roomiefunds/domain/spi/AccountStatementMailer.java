package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.person.Person;

public interface AccountStatementMailer {
    void sendAccountStatement(Person person, byte[] accountStatementPdfBytes);
}
