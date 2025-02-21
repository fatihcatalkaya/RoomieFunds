import type { PageLoad } from "./$types";

export const load = (async () => {
  return {
    label: 'Konten'
  };
}) satisfies PageLoad;