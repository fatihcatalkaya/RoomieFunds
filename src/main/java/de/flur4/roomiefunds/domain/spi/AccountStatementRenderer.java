package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.account.Account;
import de.flur4.roomiefunds.models.transaction.Transaction;

import java.util.List;

public interface AccountStatementRenderer {
    byte[] renderAccountStatement(Account account, List<Transaction> transactions);
}
