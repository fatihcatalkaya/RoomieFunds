package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.person.Person;

import java.time.LocalDate;
import java.util.List;

public interface FlurbeitragRepository {
    long getFlurbeitrag();
    void setFlurbeitrag(long flurbeitrag);
    boolean existsFlurbeitragForMonthInYear(int month, int year);
    void createFlurbeitragTransactions(ModifyingPersonDto modifyingPerson,
                                       List<Person> personList,
                                       long flurkontoAccountId,
                                       long flurbeitrag,
                                       LocalDate date);
}
