package de.flur4.roomiefunds.infrastructure.webclient.enablebanking;

import de.flur4.roomiefunds.domain.spi.EnableBankingBalanceFetcher;
import de.flur4.roomiefunds.models.webclient.enablebanking.BalanceResource;
import de.flur4.roomiefunds.models.webclient.enablebanking.BalanceStatus;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor
@JBossLog
public class EnableBankingBalanceFetcherImpl implements EnableBankingBalanceFetcher {

    @RestClient
    EnableBankingClient enableBankingClient;

    @Override
    public Optional<Long> fetchBalanceCents(String bankAccountUid) {
        var response = enableBankingClient.getAccountBalancesByAccountId(bankAccountUid);

        if (response.balances() == null || response.balances().isEmpty()) {
            log.warnf("No balances returned for bank account UID: %s", bankAccountUid);
            return Optional.empty();
        }

        // Prefer CLBD (Closing Booked), fall back to ITBD (Interim Booked)
        BalanceResource bookedBalance = null;
        for (var balance : response.balances()) {
            if (balance.balanceType() == BalanceStatus.CLBD) {
                bookedBalance = balance;
                break;
            }
            if (balance.balanceType() == BalanceStatus.ITBD && bookedBalance == null) {
                bookedBalance = balance;
            }
        }

        if (bookedBalance == null) {
            log.warnf("No booked balance (CLBD/ITBD) found for bank account UID: %s. Available types: %s",
                    bankAccountUid,
                    response.balances().stream()
                            .map(b -> b.balanceType() != null ? b.balanceType().name() : "null")
                            .toList());
            return Optional.empty();
        }

        if (bookedBalance.balanceAmount() == null || bookedBalance.balanceAmount().amount() == null) {
            log.warnf("Booked balance has no amount for bank account UID: %s", bankAccountUid);
            return Optional.empty();
        }

        long cents = new BigDecimal(bookedBalance.balanceAmount().amount())
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        return Optional.of(cents);
    }
}
