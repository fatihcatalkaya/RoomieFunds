package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public enum AddressType {
    /**
     * Business
     */
    BUSINESS,
    /**
     * Correspondence
     */
    CORRESPONDENCE,
    /**
     * DeliveryTo
     */
    DELIVERY_TO,
    /**
     * MailTo
     */
    MAIL_TO,
    /**
     * POBox
     */
    PO_BOX,
    /**
     * Postal
     */
    POSTAL,
    /**
     * Residential
     */
    RESIDENTIAL,
    /**
     * Statement
     */
    STATEMENT;
}

