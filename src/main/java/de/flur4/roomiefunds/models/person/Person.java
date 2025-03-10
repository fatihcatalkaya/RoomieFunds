package de.flur4.roomiefunds.models.person;

public record Person(long id,
                     String name,
                     String room,
                     boolean paysFloorFees,
                     long accountId,
                     boolean printOnProductTallyList,
                     String email,
                     boolean emailAccountStatement) {
}
