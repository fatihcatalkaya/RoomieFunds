import { UserManager, type UserManagerSettings, WebStorageStateStore } from 'oidc-client-ts';
import { browser } from '$app/environment';
import { AuthStore } from '$lib/stores/auth_store';
import { goto } from '$app/navigation';
import { client } from '$lib/client/client.gen';
import { get } from 'svelte/store';

let userManager: UserManager;

function parseJwt(token: string) {
    var base64Url = token.split('.')[1];
    var base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    var jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function (c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    return JSON.parse(jsonPayload);
}

client.interceptors.request.use((request) => {
    const authToken = get(AuthStore);
    if (authToken.bearerToken != '') {
        request.headers.set('Authorization', `Bearer ${authToken.bearerToken}`);
    }
    return request;
});

if (browser) {
    const config: UserManagerSettings = {
        authority: 'https://auth.flur4.de/realms/flur4.de',
        client_id: 'roomiefunds-frontend',
        stateStore: new WebStorageStateStore({ store: window.localStorage }),
        loadUserInfo: true,
        redirect_uri: `${window.location.origin}/auth-callbacks/callback`,
        post_logout_redirect_uri: `${window.location.origin}/app`,
        silent_redirect_uri: '/auth-callbacks/silent-refresh',
        monitorSession: true
    };

    userManager = new UserManager(config);
    userManager.events.addUserLoaded((loadedUser) => {
        console.log('UserManager Event: User Loaded ');
        let roles = [];
        const payload = parseJwt(loadedUser.access_token);
        if (Object.hasOwn(payload, "realm_access")) {
            if (Object.hasOwn(payload["realm_access"], "roles")) {
                roles = payload.realm_access.roles
            }
        }

        const authStoreData = {
            isAuthenticated: true,
            roles,
            bearerToken: loadedUser.access_token,
        };
        AuthStore.set(authStoreData);
    });

    userManager.events.addUserUnloaded(() => {
        console.log('UserManager Event: User Unloaded');
        const authStoreData = {
            isAuthenticated: true,
            roles: [],
            bearerToken: '',
        };
        AuthStore.set(authStoreData);
    });
}

export async function login(): Promise<void> {
    if (browser) {
        await userManager.signinRedirect();
    }
}

export async function logout(): Promise<void> {
    if (browser) {
        await userManager.signoutRedirect();
        userManager.stopSilentRenew();
    }
}

export async function handleCallback(): Promise<void> {
    if (browser) {
        await userManager.signinCallback();
        goto('/app');
    }
}

export async function handleSilentCallback(): Promise<void> {
    if (browser) {
        await userManager.signinSilentCallback();
        goto('/');
    }
}

export async function loginSilent(): Promise<void> {
    if (browser) {
        await userManager.signinSilent();
    }
}

export async function getUsername(): Promise<string | undefined> {
    if (browser) {
        const user = await userManager.getUser();
        if (user == null) {
            console.error("request to getUsername() made, but user was null");
            return undefined;
        }
        console.log(user.profile);
        return user.profile.preferred_username;
    }
}
