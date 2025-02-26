package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AccountResource(
        /** Primary account identifier */
        AccountIdentification accountId,

        /** All account identifiers provided by ASPSPs (including primary identifier available in the accountId field) */
        List<GenericIdentification> allAccountIds,

        /** Information about the financial institution servicing the account */
        FinancialInstitutionIdentification accountServicer,

        /** Account holder(s) name */
        String name,

        /** Account description set by PSU or provided by ASPSP */
        String details,

        /** Specifies the usage of the account */
        Usage usage,

        /** Specifies the type of the account */
        CashAccountType cashAccountType,

        /** Product Name of the Bank for this account, proprietary definition */
        String product,

        /** Specifies the currency of the account */
        String currency,

        /** Relationship between the PSU and the account */
        String psuStatus,

        /** Specifies credit limit of the account */
        AmountType creditLimit,

        /** Specifies whether Enable Banking is confident that the account holder is of legal age or is a minor */
        Boolean legalAge,

        /** Postal address of the account holder */
        PostalAddress postalAddress,

        /** Unique account identifier used for fetching account balances and transactions */
        String uid,

        /** Primary account identification hash */
        String identificationHash,

        /** List of possible account identification hashes */
        List<String> identificationHashes
) {
}

