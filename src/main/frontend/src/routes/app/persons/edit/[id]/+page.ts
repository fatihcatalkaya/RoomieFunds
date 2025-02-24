import { getApiPersonByPersonId } from "$lib/client";
import type { PageLoad } from "./$types";

export const prerender = false;

export const load: PageLoad = async ({ params }) => {
    return await getApiPersonByPersonId({
        path: {
            personId: Number(params.id)
        }
    });
}