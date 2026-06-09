package de.flur4.roomiefunds.models.hbci;

import java.time.LocalDate;
import java.util.Objects;

public record DateRange(LocalDate from, LocalDate to) {
    public DateRange {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        if (from.isAfter(to)) throw new IllegalArgumentException("from must be <= to");
    }
}
