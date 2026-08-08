package de.flur4.roomiefunds.infrastructure.renderer.accountstatement;

import de.flur4.roomiefunds.domain.spi.AccountStatementRenderer;
import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.transaction.Transaction;
import io.github.fatihcatalkaya.javatypst.JavaTypst;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
@RequiredArgsConstructor
public class TypstAccountStatementRenderer implements AccountStatementRenderer {

    @Location("pdf/account_statement.typ")
    Template accountStatement;
    static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(Locale.GERMANY);
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);

    @Override
    public byte[] renderAccountStatement(Account account, List<Transaction> transactions) {
        var printableTransactions = new ArrayList<>(transactions.size());
        long saldo = 0;
        StringBuilder sb = new StringBuilder();
        for (var tx : transactions) {
            String date = tx.valueDate().format(DATE_FORMATTER);
            String description = tx.description();
            String accountName = tx.targetAccountId() == account.id() ? tx.sourceAccountName() : tx.targetAccountName();
            String[] parts = accountName.split(":");
            String bookingTarget = parts[parts.length - 1];
            long previousSaldo = saldo;

            if (tx.sourceAccountActive() != tx.targetAccountActive()) {
                saldo += tx.amount();
            } else if (tx.sourceAccountId() == account.id()) {
                saldo -= tx.amount();
            } else {
                saldo += tx.amount();
            }

            // The sign shown is the direction this account's saldo moved, so the
            // amount itself is printed without one — a stored amount may be
            // negative for a booking between an Aktiv and a Passiv account.
            sb.append(saldo < previousSaldo ? '-' : '+');

            String amount = sb.append(formatCurrency(Math.abs(tx.amount()))).toString();
            String resultingBalance = formatCurrency(saldo);
            printableTransactions.add(new PrintableTransaction(date, description, bookingTarget, amount, resultingBalance, saldo < 0));
            sb.setLength(0);
        }

        var parts = account.name().split(":");
        String accountName = parts[parts.length - 1];

        String typstTemplate = accountStatement
                .data("accountName", accountName)
                .data("transactions", printableTransactions.reversed())
                .render();

        return JavaTypst.render(typstTemplate);
    }

    private String formatCurrency(long amount) {
        return CURRENCY_FORMATTER.format(BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }
}
