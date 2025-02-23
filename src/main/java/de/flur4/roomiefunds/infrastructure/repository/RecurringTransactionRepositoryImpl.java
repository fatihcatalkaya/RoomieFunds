package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.RecurringTransactionRepository;
import de.flur4.roomiefunds.models.recurringtransaction.CreateRecurringTransactionDto;
import de.flur4.roomiefunds.models.recurringtransaction.GetRecurringTransactionDto;
import de.flur4.roomiefunds.models.recurringtransaction.UpdateRecurringTransactionDto;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.ACCOUNT;
import static de.flur4.roomiefunds.infrastructure.jooq.Tables.RECURRING_TRANSACTION;
import static org.jooq.Records.mapping;

@RequiredArgsConstructor
@ApplicationScoped
public class RecurringTransactionRepositoryImpl implements RecurringTransactionRepository {
    final DSLContext jooq;

    @Override
    public Optional<GetRecurringTransactionDto> getRecurringTransactionById(long recurringTransactionId) {
        return jooq.select(
                        RECURRING_TRANSACTION.ID,
                        RECURRING_TRANSACTION.AMOUNT,
                        RECURRING_TRANSACTION.SOURCE_ACCOUNT_ID,
                        ACCOUNT.as("source_account").NAME,
                        ACCOUNT.as("source_account").IS_ACTIVE,
                        RECURRING_TRANSACTION.TARGET_ACCOUNT_ID,
                        ACCOUNT.as("target_account").NAME,
                        ACCOUNT.as("target_account").IS_ACTIVE,
                        RECURRING_TRANSACTION.VALUE_DAY_OF_MONTH,
                        RECURRING_TRANSACTION.TRANSACTION_DESCRIPTION,
                        RECURRING_TRANSACTION.NAME
                ).from(RECURRING_TRANSACTION)
                .join(ACCOUNT.as("source_account")).on(RECURRING_TRANSACTION.SOURCE_ACCOUNT_ID.eq(ACCOUNT.as("source_account").ID))
                .join(ACCOUNT.as("target_account")).on(RECURRING_TRANSACTION.TARGET_ACCOUNT_ID.eq(ACCOUNT.as("target_account").ID))
                .fetchOptional(mapping(GetRecurringTransactionDto::new));
    }

    @Override
    public List<GetRecurringTransactionDto> getAllRecurringTransactions() {
        return jooq.select(
                        RECURRING_TRANSACTION.ID,
                        RECURRING_TRANSACTION.AMOUNT,
                        RECURRING_TRANSACTION.SOURCE_ACCOUNT_ID,
                        ACCOUNT.as("source_account").NAME,
                        ACCOUNT.as("source_account").IS_ACTIVE,
                        RECURRING_TRANSACTION.TARGET_ACCOUNT_ID,
                        ACCOUNT.as("target_account").NAME,
                        ACCOUNT.as("target_account").IS_ACTIVE,
                        RECURRING_TRANSACTION.VALUE_DAY_OF_MONTH,
                        RECURRING_TRANSACTION.TRANSACTION_DESCRIPTION,
                        RECURRING_TRANSACTION.NAME
                ).from(RECURRING_TRANSACTION)
                .join(ACCOUNT.as("source_account")).on(RECURRING_TRANSACTION.SOURCE_ACCOUNT_ID.eq(ACCOUNT.as("source_account").ID))
                .join(ACCOUNT.as("target_account")).on(RECURRING_TRANSACTION.TARGET_ACCOUNT_ID.eq(ACCOUNT.as("target_account").ID))
                .orderBy(RECURRING_TRANSACTION.ID)
                .fetch(mapping(GetRecurringTransactionDto::new));
    }

    @Override
    public GetRecurringTransactionDto createRecurringTransaction(CreateRecurringTransactionDto createRecurringTransaction) {
        long newId = jooq.insertInto(RECURRING_TRANSACTION)
                .columns(
                        RECURRING_TRANSACTION.AMOUNT,
                        RECURRING_TRANSACTION.SOURCE_ACCOUNT_ID,
                        RECURRING_TRANSACTION.TARGET_ACCOUNT_ID,
                        RECURRING_TRANSACTION.VALUE_DAY_OF_MONTH,
                        RECURRING_TRANSACTION.TRANSACTION_DESCRIPTION,
                        RECURRING_TRANSACTION.NAME
                ).values(
                        createRecurringTransaction.amount(),
                        createRecurringTransaction.sourceAccountId(),
                        createRecurringTransaction.targetAccountId(),
                        createRecurringTransaction.valueDayOfMonth(),
                        createRecurringTransaction.transactionDescription(),
                        createRecurringTransaction.name()
                ).returningResult(RECURRING_TRANSACTION.ID)
                .fetchOne().value1();
        return getRecurringTransactionById(newId).get();
    }

    @Override
    public GetRecurringTransactionDto updateRecurringTransaction(long recurringTransactionId, UpdateRecurringTransactionDto updateRecurringTransaction) {
        var recurringTransaction = jooq.selectFrom(RECURRING_TRANSACTION).where(RECURRING_TRANSACTION.ID.eq(recurringTransactionId)).fetchOne();
        assert recurringTransaction != null;
        if (updateRecurringTransaction.amount().isPresent()) {
            recurringTransaction.setAmount(updateRecurringTransaction.amount().get());
        }
        if (updateRecurringTransaction.sourceAccountId().isPresent()) {
            recurringTransaction.setSourceAccountId(updateRecurringTransaction.sourceAccountId().get());
        }
        if (updateRecurringTransaction.targetAccountId().isPresent()) {
            recurringTransaction.setTargetAccountId(updateRecurringTransaction.targetAccountId().get());
        }
        if (updateRecurringTransaction.valueDayOfMonth().isPresent()) {
            recurringTransaction.setValueDayOfMonth(updateRecurringTransaction.valueDayOfMonth().get());
        }
        if (updateRecurringTransaction.name().isPresent()) {
            recurringTransaction.setName(updateRecurringTransaction.name().get());
        }
        if (updateRecurringTransaction.transactionDescription().isPresent()) {
            recurringTransaction.setTransactionDescription(updateRecurringTransaction.transactionDescription().get());
        }
        recurringTransaction.store();
        return getRecurringTransactionById(recurringTransactionId).get();
    }

    @Override
    public void deleteRecurringTransaction(long recurringTransactionId) {
        jooq.deleteFrom(RECURRING_TRANSACTION).where(RECURRING_TRANSACTION.ID.eq(recurringTransactionId)).execute();
    }
}
