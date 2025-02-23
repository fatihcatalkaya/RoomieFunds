package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import de.flur4.roomiefunds.models.transaction.CreateTransactionDto;
import de.flur4.roomiefunds.models.transaction.Transaction;
import de.flur4.roomiefunds.models.transaction.UpdateTransactionDto;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.ACCOUNT;
import static de.flur4.roomiefunds.infrastructure.jooq.Tables.TRANSACTION;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.exists;

@ApplicationScoped
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {
    final DSLContext jooq;

    @Override
    public boolean accountHasTransactions(long accountId) {
        return jooq.select(
                exists(
                        jooq.selectOne()
                                .from(TRANSACTION)
                                .where(TRANSACTION.SOURCE_ACCOUNT_ID.eq(accountId)
                                        .or(TRANSACTION.TARGET_ACCOUNT_ID.eq(accountId)))
                )
        ).fetchOne().value1().booleanValue();
    }

    @Override
    public Optional<Transaction> getTransactionById(long transactionId) {
        return jooq.select(
                        TRANSACTION.ID,
                        TRANSACTION.SOURCE_ACCOUNT_ID,
                        ACCOUNT.as("source_account").NAME,
                        ACCOUNT.as("source_account").IS_ACTIVE,
                        TRANSACTION.TARGET_ACCOUNT_ID,
                        ACCOUNT.as("target_account").NAME,
                        ACCOUNT.as("target_account").IS_ACTIVE,
                        TRANSACTION.AMOUNT,
                        TRANSACTION.CREATED_AT,
                        TRANSACTION.VALUE_DATE,
                        TRANSACTION.DESCRIPTION
                ).from(TRANSACTION)
                .join(ACCOUNT.as("source_account")).on(ACCOUNT.ID.eq(TRANSACTION.SOURCE_ACCOUNT_ID))
                .join(ACCOUNT.as("target_account")).on(ACCOUNT.ID.eq(TRANSACTION.TARGET_ACCOUNT_ID))
                .where(TRANSACTION.ID.eq(transactionId))
                .fetchOptional(mapping(Transaction::new));
    }

    @Override
    public List<Transaction> getTransactionsByAccountId(long accountId) {
        return jooq.select(
                        TRANSACTION.ID,
                        TRANSACTION.SOURCE_ACCOUNT_ID,
                        ACCOUNT.as("source_account").NAME,
                        ACCOUNT.as("source_account").IS_ACTIVE,
                        TRANSACTION.TARGET_ACCOUNT_ID,
                        ACCOUNT.as("target_account").NAME,
                        ACCOUNT.as("target_account").IS_ACTIVE,
                        TRANSACTION.AMOUNT,
                        TRANSACTION.CREATED_AT,
                        TRANSACTION.VALUE_DATE,
                        TRANSACTION.DESCRIPTION
                ).from(TRANSACTION)
                .join(ACCOUNT.as("source_account")).on(ACCOUNT.ID.eq(TRANSACTION.SOURCE_ACCOUNT_ID))
                .join(ACCOUNT.as("target_account")).on(ACCOUNT.ID.eq(TRANSACTION.TARGET_ACCOUNT_ID))
                .where(TRANSACTION.SOURCE_ACCOUNT_ID.eq(accountId).or(TRANSACTION.TARGET_ACCOUNT_ID.eq(accountId)))
                .orderBy(TRANSACTION.VALUE_DATE, TRANSACTION.CREATED_AT)
                .fetch(mapping(Transaction::new));
    }

    @Override
    public void deleteTransaction(long transactionId) {
        jooq.deleteFrom(TRANSACTION).where(TRANSACTION.ID.eq(transactionId)).execute();
    }

    @Override
    public Transaction createTransaction(CreateTransactionDto createTransactionDto) {
        long newTransactionId = jooq.insertInto(TRANSACTION).columns(
                TRANSACTION.SOURCE_ACCOUNT_ID,
                TRANSACTION.TARGET_ACCOUNT_ID,
                TRANSACTION.AMOUNT,
                TRANSACTION.VALUE_DATE,
                TRANSACTION.DESCRIPTION
        ).values(
                createTransactionDto.sourceAccountId(),
                createTransactionDto.targetAccountId(),
                createTransactionDto.amount(),
                createTransactionDto.valueDate(),
                createTransactionDto.description()
        ).returningResult(TRANSACTION.ID).fetchOne().value1();
        var transaction = getTransactionById(newTransactionId);
        assert transaction.isPresent();
        return transaction.get();
    }

    @Override
    public Transaction updateTransaction(long transactionId, UpdateTransactionDto updateTransactionDto) {
        var account = jooq.selectFrom(TRANSACTION).where(TRANSACTION.ID.eq(transactionId)).fetchOne();
        assert account != null;
        if (updateTransactionDto.sourceAccountId().isPresent()) {
            account.setSourceAccountId(updateTransactionDto.sourceAccountId().get());
        }
        if (updateTransactionDto.targetAccountId().isPresent()) {
            account.setTargetAccountId(updateTransactionDto.targetAccountId().get());
        }
        if (updateTransactionDto.amount().isPresent()) {
            account.setAmount(updateTransactionDto.amount().get());
        }
        if (updateTransactionDto.valueDate().isPresent()) {
            account.setValueDate(updateTransactionDto.valueDate().get());
        }
        if (updateTransactionDto.description().isPresent()) {
            account.setDescription(updateTransactionDto.description().get());
        }
        account.store();
        var updatedTransaction = getTransactionById(transactionId);
        assert updatedTransaction.isPresent();
        return updatedTransaction.get();
    }
}
