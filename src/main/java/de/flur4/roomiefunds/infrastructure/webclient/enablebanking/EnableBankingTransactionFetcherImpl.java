package de.flur4.roomiefunds.infrastructure.webclient.enablebanking;

import de.flur4.roomiefunds.domain.api.enablebanking.EnableBankingAuthenticationRequiredRuntimeException;
import de.flur4.roomiefunds.domain.spi.EnableBankingTransactionFetcher;
import de.flur4.roomiefunds.models.enablebanking.BankTransactionDto;
import de.flur4.roomiefunds.models.webclient.enablebanking.AccountIdentification;
import de.flur4.roomiefunds.models.webclient.enablebanking.Transaction;
import de.flur4.roomiefunds.models.webclient.enablebanking.TransactionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import de.flur4.roomiefunds.models.webclient.enablebanking.TransactionFetchStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor
@JBossLog
public class EnableBankingTransactionFetcherImpl implements EnableBankingTransactionFetcher {

    @RestClient
    EnableBankingClient enableBankingClient;

    @Override
    public List<BankTransactionDto> fetchTransactions(String bankAccountUid, LocalDate dateFrom, LocalDate dateTo) {
        List<BankTransactionDto> allTransactions = new ArrayList<>();
        String continuationKey = null;

        try {
            do {
                var response = enableBankingClient.getAccountTransactions(
                        bankAccountUid, dateFrom, dateTo, continuationKey, null, null
                );

                if (response.transactions() != null) {
                    for (var tx : response.transactions()) {
                        if (tx.status() == TransactionStatus.BOOK) {
                            allTransactions.add(mapTransaction(tx));
                        }
                    }
                }

                continuationKey = response.continuationKey();
            } while (continuationKey != null && !continuationKey.isBlank());
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 401) {
                throw new EnableBankingAuthenticationRequiredRuntimeException(bankAccountUid, e);
            }
            throw e;
        }

        return allTransactions;
    }

    @Override
    public List<BankTransactionDto> fetchTransactionsLongest(String bankAccountUid, LocalDate dateFrom) {
        List<BankTransactionDto> allTransactions = new ArrayList<>();
        String continuationKey = null;

        try {
            do {
                var response = enableBankingClient.getAccountTransactions(
                        bankAccountUid, dateFrom, null, continuationKey, null, TransactionFetchStrategy.LONGEST
                );

                if (response.transactions() != null) {
                    for (var tx : response.transactions()) {
                        if (tx.status() == TransactionStatus.BOOK) {
                            allTransactions.add(mapTransaction(tx));
                        }
                    }
                }

                continuationKey = response.continuationKey();
            } while (continuationKey != null && !continuationKey.isBlank());
        } catch (WebApplicationException e) {
            if (e.getResponse() != null && e.getResponse().getStatus() == 401) {
                throw new EnableBankingAuthenticationRequiredRuntimeException(bankAccountUid, e);
            }
            throw e;
        }

        return allTransactions;
    }

    private String extractAccountIdentifier(AccountIdentification account) {
        if (account == null) return null;
        if (account.iban() != null && !account.iban().isBlank()) return account.iban();
        if (account.other() != null && account.other().identification() != null) return account.other().identification();
        return null;
    }

    private BankTransactionDto mapTransaction(Transaction tx) {
        int amountCents = 0;
        String currency = null;
        if (tx.transactionAmount() != null) {
            currency = tx.transactionAmount().currency();
            if (tx.transactionAmount().amount() != null) {
                amountCents = new BigDecimal(tx.transactionAmount().amount())
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
            }
        }

        String creditDebitIndicator = tx.creditDebitIndicator() != null ? tx.creditDebitIndicator().name() : null;
        String creditorName = tx.creditor() != null ? tx.creditor().name() : null;
        String debtorName = tx.debtor() != null ? tx.debtor().name() : null;
        String creditorIban = extractAccountIdentifier(tx.creditorAccount());
        String debtorIban = extractAccountIdentifier(tx.debtorAccount());
        String status = tx.status() != null ? tx.status().name() : null;

        return new BankTransactionDto(
                tx.entryReference(),
                tx.transactionId(),
                amountCents,
                currency,
                creditDebitIndicator,
                tx.bookingDate(),
                tx.valueDate(),
                creditorName,
                debtorName,
                creditorIban,
                debtorIban,
                tx.remittanceInformation(),
                status
        );
    }
}
