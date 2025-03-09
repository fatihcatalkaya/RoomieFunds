import { getApiLogByTableName, getApiLogAccountTransactionsByAccountId } from "$lib/client";
import type { PageLoad } from "./$types";

const tableName = "transaction"
export const prerender = false;

export const load: PageLoad = ({ params }) => {
    const logQuery = async () => {
        let query = await getApiLogAccountTransactionsByAccountId(({
            path: { account_id: params.accountId }
        }));

        if (query.error) {
            throw query.error;
        } else {
            return query.data;
        }
    }

    return {
        streamed: {
            logQuery: logQuery()
        }
    }
}