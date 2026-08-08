import { client } from '$lib/client/client.gen';
import { getOidc } from '$lib/oidc';
import type { ClientInit } from '@sveltejs/kit';

export const init: ClientInit = async () => {
	const oidc = await getOidc();

	if (!oidc.isUserLoggedIn) {
		return;
	}

	// Keep the generated OpenAPI client's bearer token in sync with the tokens oidc-spa rotates in the
	// background. A single interceptor reads the latest value through the closure — registering a new
	// interceptor per rotation would leak one per refresh.
	let accessToken = (await oidc.getTokens()).accessToken;

	oidc.subscribeToTokensChange((tokens) => {
		accessToken = tokens.accessToken;
	});

	client.interceptors.request.use((request) => {
		request.headers.set('Authorization', `Bearer ${accessToken}`);
		return request;
	});
};
