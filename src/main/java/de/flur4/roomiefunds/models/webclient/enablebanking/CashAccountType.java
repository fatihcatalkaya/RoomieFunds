package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public enum CashAccountType {
    /**
     * Account used to post debits and credits when no specific account has been nominated
     */
    CACC,
    /**
     * Account used for the payment of cash
     */
    CASH,
    /**
     * Account used for card payments only
     */
    CARD,
    /**
     * Account used for loans
     */
    LOAN,
    /**
     * Account used for savings
     */
    SVGS,
    /**
     * Account not otherwise specified
     */
    OTHR;
}
