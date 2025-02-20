import { login } from "$lib/auth";
import type { PageLoad } from "./$types";

export const load:PageLoad = () => {
    login();
};