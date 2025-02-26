package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PostalAddress(
        AddressType addressType,
        String department,
        String subDepartment,
        String streetName,
        String buildingNumber,
        String postCode,
        String townName,
        String countrySubDivision,
        String country,
        /** Unstructured address. The two lines must embed zip code and town name */
        List<String> addressLine
) {}
