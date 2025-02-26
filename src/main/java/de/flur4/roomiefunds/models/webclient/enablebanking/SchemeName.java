package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public enum SchemeName {
    /** Clearing Identification Number */
    CHID,
    /** GS1GLN Identifier */
    GS1G,
    /** Data Universal Numbering System */
    DUNS,
    /** Bank Party Identification - Unique and unambiguous assignment made by a specific bank or similar financial institution to identify a relationship as defined between the bank and its client. */
    BANK,
    /** Tax Identification Number */
    TXID,
    /** Corporate Customer Number */
    CUST,
    /** Employer Identification Number */
    EMPL,
    /** Other Corporate - Handelsbanken-specific code */
    OTHC,
    /** Driver's License Number */
    DRLC,
    /** Customer Identification Number Individual - Handelsbanken-specific code */
    CUSI,
    /** Social Security Number */
    SOSE,
    /** Alien Registration Number */
    ARNU,
    /** Passport Number */
    CCPT,
    /** Other Individual - Handelsbanken-specific code */
    OTHI,
    /** Country Identification Code - Country authority given organisation identification (e.g., corporate registration number) */
    COID,
    /** The SIREN number is a 9-digit code assigned by INSEE, the French National Institute for Statistics and Economic Studies, to identify an organisation in France. */
    SREN,
    /** The SIRET number is a 14-digit code assigned by INSEE, the French National Institute for Statistics and Economic Studies, to identify an organisation unit in France. It consists of the SIREN number, followed by a five-digit classification number, to identify the local geographical unit of that entity. */
    SRET,
    /** National Identity Number - Number assigned by an authority to identify the national identity number of a person. */
    NIDN,
    /** OAUTH2 access token that is owned by the PISP being also an AISP and that can be used in order to identify the PSU */
    OAUT,
    /** Card PAN (masked or plain) */
    CPAN,
    /** Basic Bank Account Number - Represents a country-specific bank account number. */
    BBAN,
    /** International Bank Account Number (IBAN) - Identification used internationally by financial institutions to uniquely identify the account of a customer. */
    IBAN,
    /** Masked IBAN */
    MIBN,
    /** Swedish BankGiro account number - Used in domestic Swedish giro payments. */
    BGNR,
    /** Swedish PlusGiro account number - Used in domestic Swedish giro payments. */
    PGNR;
}

