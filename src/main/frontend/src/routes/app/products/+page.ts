import { getApiProduct } from "$lib/client";
import type { PageLoad } from "./$types";

export const load = (async () => {
  const response = await getApiProduct()
  
  if (response.error) {
    console.error(response.error)
  } else return {
    products: response.data
  };
}) satisfies PageLoad;