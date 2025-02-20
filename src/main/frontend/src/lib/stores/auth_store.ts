import { writable } from 'svelte/store';

interface AuthStoreProps {
  isAuthenticated: boolean;
  bearerToken: string;
}

export const AuthStore = writable<AuthStoreProps>({
  isAuthenticated: false,
  bearerToken: '',
});
