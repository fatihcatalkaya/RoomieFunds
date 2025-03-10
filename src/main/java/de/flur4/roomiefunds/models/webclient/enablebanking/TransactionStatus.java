package de.flur4.roomiefunds.models.webclient.enablebanking;

public enum TransactionStatus {
    /**
     * Accounted transaction (ISO20022 Closing Booked)
     */
    BOOK,

    /**
     * Cancelled transaction
     */
    CNCL,

    /**
     * Account hold
     */
    HOLD,

    /**
     * Transaction with unknown status or not fitting the other options
     */
    OTHR,

    /**
     * Instant Balance Transaction (ISO20022 Expected)
     */
    PDNG,

    /**
     * Rejected transaction
     */
    RJCT,

    /**
     * Scheduled transaction
     */
    SCHD
}
