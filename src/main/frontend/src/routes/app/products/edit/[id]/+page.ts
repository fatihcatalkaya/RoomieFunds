import { getApiProductByProductId } from "$lib/client";
import type { PageLoad } from "./$types";

export const prerender = false;

export const load: PageLoad = async ({ params }) => {
    let id = Number(params.id)
    console.log(params)
    return (await getApiProductByProductId({ path: { productId: id } }));
}