import { getApiGroup } from '$lib/client';
import type { PageLoad } from './$types';

export const load: PageLoad = () => {
	const groupQuery = async () => {
		let query = await getApiGroup();
		if (query.error) throw query.error;
		else return query.data;
	};

	return {
		streamed: {
			groupQuery: groupQuery()
		}
	};
};
