import { createAccountsQuery } from './accountsQuery';
import type { PageLoad } from './$types';

export const load: PageLoad = () => {
	return {
		accountsQuery: createAccountsQuery(false)
	};
};
