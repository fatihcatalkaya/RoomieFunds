import { goto } from "$app/navigation";
import type { PageLoad } from "./$types";

export const load = (async () => {
    goto('/app/persons');
    return {};
}) satisfies PageLoad;