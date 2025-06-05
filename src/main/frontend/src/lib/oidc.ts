import { createOidc } from 'oidc-spa';

const prOidc = createOidc({
	issuerUri: 'https://auth.flur4.de/realms/flur4.de',
	clientId: 'roomiefunds-frontend',
	homeUrl: window.location.origin,
});

export const oidcClient = await prOidc;
