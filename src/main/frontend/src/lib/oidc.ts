import { createOidc, type Oidc } from 'oidc-spa';

export class OidcWrapper {
	static instance: OidcWrapper;
	oidcClient: Promise<Oidc.LoggedIn<Record<string, unknown>> | Oidc.NotLoggedIn>;

	private constructor() {
		this.oidcClient = createOidc({
			issuerUri: 'https://auth.flur4.de/realms/flur4.de',
			clientId: 'roomiefunds-frontend',
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
