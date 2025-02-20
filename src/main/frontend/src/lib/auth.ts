import { UserManager, type UserManagerSettings, WebStorageStateStore } from 'oidc-client-ts';
import { browser } from '$app/environment';
import { AuthStore } from '$lib/stores/auth_store';
import { goto } from '$app/navigation';
import { client } from '$lib/client/client.gen';
import { get } from 'svelte/store';

let userManager: UserManager;

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
    //post_logout_redirect_uri: 'https://funds.flur4.de',
    silent_redirect_uri: '/auth-callbacks/silent-refresh',
    monitorSession: true
  };

  userManager = new UserManager(config);
  userManager.events.addUserLoaded((loadedUser) => {
    console.log('UserManager Event: User Loaded ');
    const authStoreData = {
      isAuthenticated: true,
      bearerToken: loadedUser.access_token,
    };
    AuthStore.set(authStoreData);
  });

  userManager.events.addUserUnloaded(() => {
    console.log('UserManager Event: User Unloaded');
    const authStoreData = {
      isAuthenticated: true,
      bearerToken: '',
    };
    AuthStore.set(authStoreData);
  });
}

async function login(): Promise<void> {
  if (browser) {
    await userManager.signinRedirect();
  }
}

async function logout(): Promise<void> {
  if (browser) {
    await userManager.signoutRedirect();
    userManager.stopSilentRenew();
  }
}

async function handleCallback(): Promise<void> {
  if (browser) {
    await userManager.signinCallback();
    goto('/app');
  }
}

async function handleSilentCallback(): Promise<void> {
  if (browser) {
    await userManager.signinSilentCallback();
    goto('/');
  }
}

async function loginSilent(): Promise<void> {
  if (browser) {
    await userManager.signinSilent();
  }
}

async function getUsername(): Promise<string | undefined> {
  if (browser) {
    const user = await userManager.getUser();
    if(user == null) {
        console.error("request to getUsername() made, but user was null");
        return undefined;
    }
    return user.profile.preferred_username;
  }
}

export { login, logout, handleCallback, handleSilentCallback, loginSilent, getUsername };
