<script lang="ts">
	import Breadcrumb from '$lib/components/Breadcrumb.svelte';
	import { fade } from 'svelte/transition';
	import MdiLogout from '~icons/mdi/logout';
	import MdiPerson from '~icons/mdi/person';
	import MdiExpand from '~icons/mdi/arrow-expand-horizontal';
	import MdiCollapse from '~icons/mdi/arrow-collapse-horizontal';
	import { oidcClient, performLogin } from '$lib/oidc';
	import { onMount } from 'svelte';
	import { client } from '$lib/client/client.gen';
	import { AuthStore } from '$lib/stores/auth_store';

	let username: string | null = $state<string | null>(null);
	let expanded: boolean = $state(false);

	onMount(async () => {
		if (!oidcClient.isUserLoggedIn) {
			performLogin();
		} else {
			oidcClient.subscribeToTokensChange((tokens) => {
				client.interceptors.request.use((request) => {
					request.headers.set('Authorization', `Bearer ${tokens.accessToken}`);
					return request;
				});
			});

			const accessToken = (await oidcClient.getTokens_next()).accessToken;
			client.interceptors.request.use((request) => {
				request.headers.set('Authorization', `Bearer ${accessToken}`);
				return request;
			});
		}
	});

	const logout = () => {
		oidcClient.logout({ redirectTo: 'specific url', url: '/' });
	};

	let { children } = $props();
</script>

{#if oidcClient.isUserLoggedIn}
	<div class="flex h-screen w-screen flex-col">
		<div class="h-12 w-screen bg-white/60 shadow-sm">
			<div class="mx-2 flex h-full flex-row items-center gap-2">
				<span class="text-xl font-bold">RoomieFunds</span>
				<label class="swap m-0 h-8 w-8 p-0 text-xl max-md:hidden">
					<input type="checkbox" bind:checked={expanded} defaultChecked="true" />
					<div class="swap-on m-0 p-0"><MdiCollapse /></div>
					<div class="swap-off m-0 p-0"><MdiExpand /></div>
				</label>
				<div class="flex-1"></div>
				{#if username}
					<span class="flex items-center sm:text-base md:text-xl"> [<MdiPerson /> {username}]</span>
				{/if}
				<button
					type="button"
					onclick={logout}
					class="w-10 rounded-lg border border-red-500 bg-red-500 px-2 py-2 text-center text-lg text-white shadow-sm transition-all hover:border-red-700 hover:bg-red-700 focus:ring focus:ring-red-200 disabled:cursor-not-allowed disabled:border-red-300 disabled:bg-red-300"
					><MdiLogout /></button
				>
			</div>
		</div>
		<div class="mx-auto w-full {expanded ? '' : 'max-w-6xl'} px-2" style="min-height: 0;">
			<Breadcrumb />
			{@render children()}
			<div class="h-8"></div>
		</div>
	</div>
{:else}
	<div
		class="bg-base-100 absolute inset-0 z-100 flex h-screen w-screen flex-col transition-opacity transition-normal"
		out:fade
	>
		<div class="gap align-center m-auto flex flex-col">
			<span class="loading loading-dots loading-lg mx-auto"></span>
			Warten auf Authentifizierung
		</div>
	</div>
{/if}
