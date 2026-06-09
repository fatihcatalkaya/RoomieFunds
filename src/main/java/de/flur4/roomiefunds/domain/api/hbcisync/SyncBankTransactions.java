package de.flur4.roomiefunds.domain.api.hbcisync;

import de.flur4.roomiefunds.models.hbci.HbciSyncResult;

public interface SyncBankTransactions {
    HbciSyncResult sync(long accountId);
}
