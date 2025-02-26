package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public enum BalanceStatus {
    /**
     * (ISO20022 Closing Available) Closing available balance
     */
    CLAV,

    /**
     * (ISO20022 ClosingBooked) Accounting Balance
     */
    CLBD,

    /**
     * (ISO20022 ForwardAvailable) Balance that is at the disposal of account
     * holders on the date specified
     */
    FWAV,

    /**
     * (ISO20022 Information) Balance for informational purposes
     */
    INFO,

    /**
     * (ISO20022 InterimAvailable) Available balance calculated in the course
     * of the day
     */
    ITAV,

    /**
     * (ISO20022 InterimBooked) Booked balance calculated in the course of
     * the day
     */
    ITBD,

    /**
     * (ISO20022 OpeningAvailable) Opening balance that is at the disposal
     * of account holders at the beginning of the date specified
     */
    OPAV,

    /**
     * (ISO20022 OpeningBooked) Book balance of the account at the beginning
     * of the account reporting period. It always equals the closing book balance
     * from the previous report
     */
    OPBD,

    /**
     * Other Balance
     */
    OTHR,

    /**
     * (ISO20022 PreviouslyClosedBooked) Balance of the account at the end
     * of the previous reporting period
     */
    PRCD,

    /**
     * Value-date balance
     */
    VALU,

    /**
     * (ISO20022 Expected) Instant Balance
     */
    XPCD;
}
