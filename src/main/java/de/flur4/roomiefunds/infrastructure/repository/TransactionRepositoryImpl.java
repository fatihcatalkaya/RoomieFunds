package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.TRANSACTION;
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
}
