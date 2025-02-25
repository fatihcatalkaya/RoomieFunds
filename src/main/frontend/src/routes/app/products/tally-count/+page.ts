import { getApiFlurkonto, getApiProduct } from "$lib/client";
import type { PageLoad } from "../$types";

export const load: PageLoad = async () => {
    const productsQuery = getApiProduct();
    const flurkontoQuery = getApiFlurkonto();

    const [ products, flurkonto ] = await Promise.all([productsQuery, flurkontoQuery]);

    return {
        products: products.data,
        mainAccount: flurkonto.data,
    }
}
