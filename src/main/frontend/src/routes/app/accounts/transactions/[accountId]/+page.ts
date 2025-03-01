import type { PageLoad } from "./$types";
import { createFetchQuery } from "./transactionsLoadQuery";

export const prerender = false;

export const load: PageLoad = ({ params }) => {
    return {
        fetchQuery: createFetchQuery(Number(params.accountId))
    }
}