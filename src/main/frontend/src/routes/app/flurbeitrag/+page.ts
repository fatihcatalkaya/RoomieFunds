import { getApiAccount, getApiFlurbeitrag, getApiKontoFlurkonto } from '$lib/client';
import type { PageLoad } from './$types';

export const prerender = false;

export const load: PageLoad = async () => {
	const [flurbeitrag, flurkonto, accounts] = await Promise.all([
		getApiFlurbeitrag(),
		getApiKontoFlurkonto(),
		getApiAccount()
	]);

	return { flurbeitrag, flurkonto, accounts };
};
