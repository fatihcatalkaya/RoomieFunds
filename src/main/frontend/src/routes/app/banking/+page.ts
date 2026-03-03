import { getApiEnablebankingSession, getApiEnablebankingSyncStatus, type EnableBankingSession, type SessionSyncStatus } from "$lib/client";
import type { PageLoad } from "./$types";

export const load: PageLoad = () => {
    const createBankingSessionsQuery: () => Promise<EnableBankingSession[]> = async () => {
        const query = await getApiEnablebankingSession();

        if (query.error) {
            throw query.error;
        } else {
            return query.data!;
        }
    }

    const createSyncStatusQuery: () => Promise<SessionSyncStatus[]> = async () => {
        const query = await getApiEnablebankingSyncStatus();

        if (query.error) {
            return [];
        } else {
            return query.data!;
        }
    }

    return {
        streamed: {
            bankingSessionsQuery: createBankingSessionsQuery(),
            syncStatusQuery: createSyncStatusQuery()
        }
    }
}