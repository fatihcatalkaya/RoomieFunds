import { getApiEnablebanking } from "$lib/client";
import type { PageLoad } from "./$types";

export const load: PageLoad = async () => {
    const createASPSPsQuery = async () => {
        const query = await getApiEnablebanking();

        if (query.error) {
            throw query.error;
        } else {
            return query.data!;
        }
    }

    return {
        aspsps: await createASPSPsQuery(),
    }
}