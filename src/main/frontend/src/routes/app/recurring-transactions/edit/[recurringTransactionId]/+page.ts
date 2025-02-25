import { getApiRecurringTransactionByRecurringTransactionId } from "$lib/client";
import type { PageLoad } from "./$types";

export const prerender = false;

export const load: PageLoad = async ({ params }) => {
    let recurringTransactionId = Number(params.recurringTransactionId)
    return (await getApiRecurringTransactionByRecurringTransactionId({ path: { recurringTransactionId } }));
}