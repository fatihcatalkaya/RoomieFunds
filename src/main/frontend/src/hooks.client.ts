import { client } from '$lib/client/client.gen';
import { oidcClient } from '$lib/oidc';
import type { ClientInit } from '@sveltejs/kit';

export const init: ClientInit = async () => {
	if (oidcClient.isUserLoggedIn) {
		// Tell the OIDC client to update the OpenAPI client bearer token
		oidcClient.subscribeToTokensChange((tokens) => {
			client.interceptors.request.use((request) => {
				request.headers.set('Authorization', `Bearer ${tokens.accessToken}`);
				return request;
			});
		});

		// To make all requests immediately authenticated, we set the accesstoken
		// once manually
		const accessToken = (await oidcClient.getTokens_next()).accessToken;
		client.interceptors.request.use((request) => {
			request.headers.set('Authorization', `Bearer ${accessToken}`);
			return request;
		});
	}
};
