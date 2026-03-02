package de.flur4.roomiefunds.models.person;

public record Person(long id,
                     String firstName,
                     String lastName,
                     String room,
                     boolean paysFloorFees,
                     long accountId,
                     boolean printOnProductTallyList,
                     String email,
                     boolean emailAccountStatement,
                     String keycloakUserId) {
}
