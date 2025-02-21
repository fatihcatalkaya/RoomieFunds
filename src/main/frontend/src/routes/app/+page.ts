import type { PageLoad } from "./$types";

export const load = (async () => {
  return {
    label: 'Home'
  };
}) satisfies PageLoad;