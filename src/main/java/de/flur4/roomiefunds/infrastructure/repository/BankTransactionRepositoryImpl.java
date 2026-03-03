package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.BankTransactionRepository;
import de.flur4.roomiefunds.models.enablebanking.BankTransactionDto;
import de.flur4.roomiefunds.models.enablebanking.BankTransactionEntity;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor
@JBossLog
public class BankTransactionRepositoryImpl implements BankTransactionRepository {
    final DSLContext jooq;

    private static final Table<Record> BANK_TRANSACTION = DSL.table("bank_transaction");
    private static final Field<Long> BT_ID = DSL.field("id", Long.class);
    private static final Field<Long> BT_SESSION_ID = DSL.field("session_id", Long.class);
    private static final Field<String> BT_ENTRY_REFERENCE = DSL.field("entry_reference", String.class);
    private static final Field<String> BT_TRANSACTION_ID = DSL.field("transaction_id", String.class);
    private static final Field<Integer> BT_AMOUNT_CENTS = DSL.field("amount_cents", Integer.class);
    private static final Field<String> BT_CURRENCY = DSL.field("currency", String.class);
    private static final Field<String> BT_CREDIT_DEBIT_INDICATOR = DSL.field("credit_debit_indicator", String.class);
    private static final Field<LocalDate> BT_BOOKING_DATE = DSL.field("booking_date", LocalDate.class);
    private static final Field<LocalDate> BT_VALUE_DATE = DSL.field("value_date", LocalDate.class);
    private static final Field<String> BT_CREDITOR_NAME = DSL.field("creditor_name", String.class);
    private static final Field<String> BT_DEBTOR_NAME = DSL.field("debtor_name", String.class);
    private static final Field<String> BT_CREDITOR_IBAN = DSL.field("creditor_iban", String.class);
    private static final Field<String> BT_DEBTOR_IBAN = DSL.field("debtor_iban", String.class);
    private static final Field<String> BT_REMITTANCE_INFORMATION = DSL.field("remittance_information", String.class);
    private static final Field<String> BT_STATUS = DSL.field("status", String.class);
    private static final Field<OffsetDateTime> BT_CREATED_AT = DSL.field("created_at", OffsetDateTime.class);

    @Override
    public int insertTransactions(long sessionId, List<BankTransactionDto> transactions) {
        if (transactions.isEmpty()) {
            return 0;
        }

        int inserted = 0;
        for (var tx : transactions) {
            String remittanceInfo = tx.remittanceInformation() != null
                    ? String.join("\n", tx.remittanceInformation())
                    : null;

            int result = jooq.insertInto(BANK_TRANSACTION)
                    .columns(BT_SESSION_ID, BT_ENTRY_REFERENCE, BT_TRANSACTION_ID, BT_AMOUNT_CENTS,
                            BT_CURRENCY, BT_CREDIT_DEBIT_INDICATOR, BT_BOOKING_DATE, BT_VALUE_DATE,
                            BT_CREDITOR_NAME, BT_DEBTOR_NAME, BT_CREDITOR_IBAN, BT_DEBTOR_IBAN,
                            BT_REMITTANCE_INFORMATION, BT_STATUS)
                    .values(sessionId, tx.entryReference(), tx.transactionId(), tx.amountCents(),
                            tx.currency(), tx.creditDebitIndicator(), tx.bookingDate(), tx.valueDate(),
                            tx.creditorName(), tx.debtorName(), tx.creditorIban(), tx.debtorIban(),
                            remittanceInfo, tx.status())
                    .execute();
            inserted += result;
        }
        return inserted;
    }

    @Override
    public int deleteTransactionsInRange(long sessionId, LocalDate dateFrom, LocalDate dateTo) {
        return jooq.deleteFrom(BANK_TRANSACTION)
                .where(BT_SESSION_ID.eq(sessionId))
                .and(BT_BOOKING_DATE.greaterOrEqual(dateFrom))
                .and(BT_BOOKING_DATE.lessOrEqual(dateTo))
                .execute();
    }

    @Override
    public long computeTransactionSum(long sessionId) {
        // SUM of CRDT amounts minus SUM of DBIT amounts
        Field<Long> balanceField = DSL.coalesce(
                DSL.sum(DSL.when(BT_CREDIT_DEBIT_INDICATOR.eq("CRDT"), BT_AMOUNT_CENTS.cast(SQLDataType.BIGINT))
                        .otherwise(BT_AMOUNT_CENTS.cast(SQLDataType.BIGINT).neg())).cast(SQLDataType.BIGINT),
                DSL.inline(0L)
        );

        var result = jooq.select(balanceField)
                .from(BANK_TRANSACTION)
                .where(BT_SESSION_ID.eq(sessionId))
                .fetchOne();

        return result != null && result.value1() != null ? result.value1() : 0L;
    }

    @Override
    public Optional<LocalDate> getLastBookingDate(long sessionId) {
        return jooq.select(DSL.max(BT_BOOKING_DATE))
                .from(BANK_TRANSACTION)
                .where(BT_SESSION_ID.eq(sessionId))
                .fetchOptional()
                .map(r -> r.value1());
    }

    @Override
    public List<BankTransactionEntity> getTransactionsBySession(long sessionId) {
        return jooq.select(BT_ID, BT_SESSION_ID, BT_ENTRY_REFERENCE, BT_TRANSACTION_ID, BT_AMOUNT_CENTS,
                        BT_CURRENCY, BT_CREDIT_DEBIT_INDICATOR, BT_BOOKING_DATE, BT_VALUE_DATE,
                        BT_CREDITOR_NAME, BT_DEBTOR_NAME, BT_CREDITOR_IBAN, BT_DEBTOR_IBAN,
                        BT_REMITTANCE_INFORMATION, BT_STATUS, BT_CREATED_AT)
                .from(BANK_TRANSACTION)
                .where(BT_SESSION_ID.eq(sessionId))
                .orderBy(BT_BOOKING_DATE.desc())
                .fetch(r -> new BankTransactionEntity(
                        r.get(BT_ID),
                        r.get(BT_SESSION_ID),
                        r.get(BT_ENTRY_REFERENCE),
                        r.get(BT_TRANSACTION_ID),
                        r.get(BT_AMOUNT_CENTS),
                        r.get(BT_CURRENCY),
                        r.get(BT_CREDIT_DEBIT_INDICATOR),
                        r.get(BT_BOOKING_DATE),
                        r.get(BT_VALUE_DATE),
                        r.get(BT_CREDITOR_NAME),
                        r.get(BT_DEBTOR_NAME),
                        r.get(BT_CREDITOR_IBAN),
                        r.get(BT_DEBTOR_IBAN),
                        splitRemittanceInformation(r.get(BT_REMITTANCE_INFORMATION)),
                        r.get(BT_STATUS),
                        r.get(BT_CREATED_AT)
                ));
    }

    private List<String> splitRemittanceInformation(String remittanceInfo) {
        if (remittanceInfo == null || remittanceInfo.isBlank()) {
            return List.of();
        }
        return Arrays.asList(remittanceInfo.split("\n"));
    }
}
