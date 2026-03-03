package de.flur4.roomiefunds.domain.api.enablebanking;

import de.flur4.roomiefunds.models.enablebanking.SyncResult;

import java.util.List;

public interface SyncBankTransactions {
    List<SyncResult> syncAllSessions();

    SyncResult syncSession(long sessionId);
}
