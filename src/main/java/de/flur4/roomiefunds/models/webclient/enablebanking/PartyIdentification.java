package de.flur4.roomiefunds.models.webclient.enablebanking;

public record PartyIdentification(
        String name,
        PostalAddress postalAddress,
        GenericIdentification organisationId,
        GenericIdentification privateId,
        ContactDetails contactDetails
) {}
