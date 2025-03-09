import { getApiLogByTableName, type LogEntryDto } from "$lib/client";
import type { PageLoad } from "./$types";

const tableName = "product"

export const load: PageLoad = () => {
    const logQuery = async () => {
        let query = await getApiLogByTableName(({
            path: { table_name: tableName }
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