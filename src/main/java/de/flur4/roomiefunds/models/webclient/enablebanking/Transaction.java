package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDate;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Transaction(
        String entryReference,
        String merchantCategoryCode,
        AmountType transactionAmount,
        PartyIdentification creditor,
        AccountIdentification creditorAccount,
        FinancialInstitutionIdentification creditorAgent,
        PartyIdentification debtor,
        AccountIdentification debtorAccount,
        FinancialInstitutionIdentification debtorAgent,
        BankTransactionCode bankTransactionCode,
        CreditDebitIndicator creditDebitIndicator,
        TransactionStatus status,
        LocalDate bookingDate,
        LocalDate valueDate,
        LocalDate transactionDate,
        AmountType balanceAfterTransaction,
        String referenceNumber,
        List<String> remittanceInformation,
        List<GenericIdentification> debtorAccountAdditionalIdentification,
        List<GenericIdentification> creditorAccountAdditionalIdentification,
        ExchangeRate exchangeRate,
        String note,
        String transactionId
) {
}
