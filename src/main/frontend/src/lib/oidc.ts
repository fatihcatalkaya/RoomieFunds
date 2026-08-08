import { createOidc, oidcEarlyInit, type Oidc } from 'oidc-spa/core';

// Must run before anything else inspects the URL, because oidc-spa picks the auth response out of it.
// The oidc-spa Vite plugin would normally do this from a generated client entrypoint, but it resolves
// that entrypoint from a root index.html which SvelteKit does not have. So we use the documented
// "Manual - Easy" setup: https://docs.oidc-spa.dev/v/v10/integration-guides/usage
oidcEarlyInit({ BASE_URL: import.meta.env.BASE_URL });

/** Mirrors OidcConfigurationDto served by the backend at GET /api/config/oidc. */
type OidcConfiguration = {
	issuerUri: string;
	clientId: string;
};

/**
 * The backend owns the OIDC parameters so one built bundle works in every environment.
 * Uses plain fetch rather than the generated OpenAPI client: this runs before the client's bearer
 * token interceptor is installed, and the endpoint needs no authentication anyway.
 */
async function fetchOidcConfiguration(): Promise<OidcConfiguration> {
	const response = await fetch('/api/config/oidc');

	if (!response.ok) {
		throw new Error(
			`Could not load the OIDC configuration from the backend: ${response.status} ${response.statusText}`
		);
	}

	return (await response.json()) as OidcConfiguration;
}

const prOidc: Promise<Oidc> = fetchOidcConfiguration().then(({ issuerUri, clientId }) =>
	createOidc({ issuerUri, clientId })
);

export async function getOidc(): Promise<Oidc> {
	return prOidc;
}
