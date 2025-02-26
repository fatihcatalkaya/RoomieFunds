import { getApiRecurringTransaction } from "$lib/client";
import type { PageLoad } from "./$types";

export const load: PageLoad = () => {
    const recurringQuery = async () => {
		let query = await getApiRecurringTransaction();

		if (query.error) {
			throw query.error;
		} else {
			return query.data;
		}
	};

    return {
        recurringQuery: recurringQuery()
    }
}