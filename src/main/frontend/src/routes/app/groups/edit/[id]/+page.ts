import { getApiGroupByGroupId } from '$lib/client';
import type { PageLoad } from './$types';

export const prerender = false;

export const load: PageLoad = async ({ params }) => {
	const groupQuery = getApiGroupByGroupId({
		path: {
			groupId: Number(params.id)
		}
	});

	return {
		group: await groupQuery
	};
};
