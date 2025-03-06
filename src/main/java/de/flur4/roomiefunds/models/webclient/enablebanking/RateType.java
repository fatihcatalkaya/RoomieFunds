package de.flur4.roomiefunds.models.webclient.enablebanking;

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
