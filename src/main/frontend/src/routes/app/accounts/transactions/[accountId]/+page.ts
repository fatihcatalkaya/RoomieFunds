import type { PageLoad } from "./$types";
import { createFetchQuery } from "./transactionsLoadQuery";

export const load: PageLoad = ({ params }) => {
    return {
        fetchQuery: createFetchQuery(Number(params.accountId))
    }
}