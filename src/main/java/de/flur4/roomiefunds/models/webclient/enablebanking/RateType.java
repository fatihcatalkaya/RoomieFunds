package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public enum RateType {
    /**
     * Exchange rate applied is the rate agreed between the parties
     */
    AGRD,

    /**
     * Exchange rate applied is the market rate at the time of the sale.
     */
    SALE,

    /**
     * Exchange rate applied is the spot rate.
     */
    SPOT
}
