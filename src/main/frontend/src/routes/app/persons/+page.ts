import { getApiPerson, type Person } from "$lib/client";
import type { PageLoad } from "./$types";

export const load: PageLoad = () => {
    const personQuery = async () => {
        let query = await getApiPerson();

		if (query.error) {
			throw query.error;
		} else {
			return query.data;
		}
    }

    return {
        streamed: {
            personQuery: personQuery()
        }
    }
}