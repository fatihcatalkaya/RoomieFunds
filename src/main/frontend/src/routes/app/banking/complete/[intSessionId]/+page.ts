import { getApiAccount, getApiEnablebankingSessionUnfinishedBySessionId, type EnableBankingUnfinishedSession } from "$lib/client";
import type { PageLoad } from "./$types";

export const load: PageLoad = ({ params }) => {
    const createUnfinishedSessionQuery: () => Promise<EnableBankingUnfinishedSession> = async () => {
        const query = await getApiEnablebankingSessionUnfinishedBySessionId({
            path: {
                sessionId: Number(params.intSessionId),
            }
        });

        if (query.error) {
            throw query.error;
        } else {
            return query.data!;
        }
    }

    const createAccountsQuery = async () => {
        const query = await getApiAccount();

        if (query.error) {
            throw query.error;
        } else {
            return query.data!;
        }
    }

    return {
        streamed: {
            unfinishedSessionQuery: createUnfinishedSessionQuery(),
            accountsQuery: createAccountsQuery(),
        }
    }
}