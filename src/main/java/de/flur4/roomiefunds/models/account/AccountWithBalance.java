package de.flur4.roomiefunds.models.account;

public record AccountWithBalance(long id,
                                 String name,
                                 boolean active,
                                 double balance) {
}
