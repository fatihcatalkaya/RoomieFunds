import { getApiFlurkonto, getApiProduct } from "$lib/client";
import type { PageLoad } from "../$types";

export const load: PageLoad = async () => {
    return {
        productsQuery: await getApiProduct(),
        flurkontoQuery: await getApiFlurkonto(),
    }
}
