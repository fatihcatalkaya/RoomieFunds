import { writable } from 'svelte/store';

interface AuthStoreProps {
  isAuthenticated: boolean;
  roles: string[];
  bearerToken: string;
}

export const AuthStore = writable<AuthStoreProps>({
  isAuthenticated: false,
  roles: [],
  bearerToken: '',
});
