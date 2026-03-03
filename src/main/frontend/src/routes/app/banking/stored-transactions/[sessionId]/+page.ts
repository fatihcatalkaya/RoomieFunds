import { getApiEnablebankingSessionBySessionIdStoredTransactions, type BankTransactionEntity } from "$lib/client";
import type { PageLoad } from "./$types";

export const load: PageLoad = ({ params }) => {
    const createTransactionsQuery: () => Promise<BankTransactionEntity[]> = async () => {
        const query = await getApiEnablebankingSessionBySessionIdStoredTransactions({
            path: { sessionId: Number(params.sessionId) }
        });

        if (query.error) {
            throw query.error;
        } else {
            return query.data!;
        }
    }

    return {
        streamed: {
            transactionsQuery: createTransactionsQuery()
        },
        sessionId: Number(params.sessionId)
    }
}
