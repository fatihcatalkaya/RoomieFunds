import { getApiEnablebankingSession, type EnableBankingSession } from "$lib/client";
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

    return {
        streamed: {
            bankingSessionsQuery: createBankingSessionsQuery()
        }
    }
}