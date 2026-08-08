import { createOidc, type Oidc } from 'oidc-spa';

// `vite dev` talks to the Keycloak from docker-compose.yaml, every other build to production.
// Override either one with VITE_OIDC_ISSUER_URI / VITE_OIDC_CLIENT_ID, see .env.example.
const DEFAULT_ISSUER_URI = import.meta.env.DEV
	? 'http://localhost:9090/realms/roomiefunds'
	: 'https://auth.flur4.de/realms/flur4.de';

const ISSUER_URI: string = import.meta.env.VITE_OIDC_ISSUER_URI ?? DEFAULT_ISSUER_URI;
const CLIENT_ID: string = import.meta.env.VITE_OIDC_CLIENT_ID ?? 'roomiefunds-frontend';

export class OidcWrapper {
	static instance: OidcWrapper;
	oidcClient: Promise<Oidc.LoggedIn<Record<string, unknown>> | Oidc.NotLoggedIn>;

	private constructor() {
		this.oidcClient = createOidc({
			issuerUri: ISSUER_URI,
			clientId: CLIENT_ID,
			homeUrl: window.location.origin
		});
	}

	public static getInstance(): OidcWrapper {
		if (this.instance === undefined || this.instance === null) {
			this.instance = new OidcWrapper();
		}
		return this.instance;
	}

	async getOidcClient(): Promise<Promise<Oidc.LoggedIn<Record<string, unknown>> | Oidc.NotLoggedIn>> {
		return this.oidcClient;
	}
}
