<script lang="ts">
    import { goto } from '$app/navigation';
    import { getUsername, loginSilent, logout } from '$lib/auth';
	import Breadcrumb from '$lib/components/Breadcrumb.svelte';
    import { AuthStore } from '$lib/stores/auth_store';
    import { onMount } from 'svelte';
    import { get } from 'svelte/store';
    import MdiLogout from '~icons/mdi/logout';
    import MdiPerson from '~icons/mdi/person';
  
    let username: string | null = $state<string | null>(null);
  
    function triggerLogout() {
      logout();
    }
  
    onMount(() => {
      let authState = get(AuthStore);
      if (!authState.isAuthenticated) {
        loginSilent()
          .then(() => {
            authState = get(AuthStore);
            if (!authState.isAuthenticated) {
              goto('/');
            } else {
              getUsername().then((name) => (username = name!));
            }
          })
          .catch(() => {
            goto('/');
          });
      } else {
        getUsername().then((name) => (username = name!));
      }
    });
  
    let { children } = $props();
  </script>
  
  {#if $AuthStore.isAuthenticated}
      <div class="flex h-screen w-screen flex-col">
        <div class="h-12 w-screen bg-white/60 shadow-sm">
          <div class="mx-2 flex h-full flex-row items-center">
            <span class="mr-auto text-xl font-bold">RoomieFunds</span>
            {#if username}
              <span class="mr-2 flex items-center sm:text-base md:text-xl"
                > [<MdiPerson /> {username}]</span
              >
            {/if}
            <button
              type="button"
              onclick={triggerLogout}
              class="ml-2 w-10 rounded-lg border border-red-500 bg-red-500 px-2 py-2 text-center text-lg text-white shadow-sm transition-all hover:border-red-700 hover:bg-red-700 focus:ring focus:ring-red-200 disabled:cursor-not-allowed disabled:border-red-300 disabled:bg-red-300"
              ><MdiLogout /></button
            >
          </div>
        </div>
        <div class="mx-auto w-full max-w-6xl px-2" style="min-height: 0;">
          <Breadcrumb />
          {@render children()}
        </div>
      </div>
  {/if}
  