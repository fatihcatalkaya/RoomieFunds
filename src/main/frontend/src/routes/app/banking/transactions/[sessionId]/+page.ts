import { getApiEnablebankingSessionBySessionIdTransactions, type BankTransactionsResult } from "$lib/client";
import type { PageLoad } from "./$types";

export const load: PageLoad = ({ params }) => {
    const now = new Date();
    const ninetyDaysAgo = new Date(now);
    ninetyDaysAgo.setDate(ninetyDaysAgo.getDate() - 90);

    const dateFrom = ninetyDaysAgo.toISOString().split('T')[0];
    const dateTo = now.toISOString().split('T')[0];

    const createTransactionsQuery: () => Promise<BankTransactionsResult> = async () => {
        const query = await getApiEnablebankingSessionBySessionIdTransactions({
            path: { sessionId: Number(params.sessionId) },
            query: { dateFrom, dateTo }
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
        initialDateFrom: dateFrom,
        initialDateTo: dateTo,
        sessionId: Number(params.sessionId)
    }
}
