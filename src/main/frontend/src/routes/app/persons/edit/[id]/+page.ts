import { getApiPersonByPersonId } from "$lib/client";
import type { PageLoad } from "./$types";

export const prerender = false;

export const load: PageLoad = async ({ params }) => {
    const personQuery = getApiPersonByPersonId({
        path: {
            personId: Number(params.id)
        }
    });

    return {
        person: await personQuery
    };
}