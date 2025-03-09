<script lang="ts">
	import { goto } from '$app/navigation';
	import { getUsername, loginSilent, logout } from '$lib/auth';
	import Breadcrumb from '$lib/components/Breadcrumb.svelte';
	import { AuthStore } from '$lib/stores/auth_store';
	import { onMount } from 'svelte';
	import { get } from 'svelte/store';
	import { fade } from 'svelte/transition';
	import MdiLogout from '~icons/mdi/logout';
	import MdiPerson from '~icons/mdi/person';
	import MdiExpand from '~icons/mdi/arrow-expand-horizontal';
	import MdiCollapse from '~icons/mdi/arrow-collapse-horizontal';

	let username: string | null = $state<string | null>(null);
	let expanded: boolean = $state(false);

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
			<div class="mx-2 flex h-full flex-row gap-2 items-center">
				<span class="text-xl font-bold">RoomieFunds</span>
				<label class="swap w-8 h-8 p-0 m-0 text-xl max-md:hidden">
					<input type="checkbox" bind:checked={expanded} defaultChecked=true />
					<div class="swap-on p-0 m-0"><MdiCollapse /></div>
					<div class="swap-off p-0 m-0"><MdiExpand /></div>
				</label>
				<div class="flex-1"></div>
				{#if username}
					<span class="flex items-center sm:text-base md:text-xl">
						[<MdiPerson /> {username}]</span
					>
				{/if}
				<button
					type="button"
					onclick={triggerLogout}
					class="w-10 rounded-lg border border-red-500 bg-red-500 px-2 py-2 text-center text-lg text-white shadow-sm transition-all hover:border-red-700 hover:bg-red-700 focus:ring focus:ring-red-200 disabled:cursor-not-allowed disabled:border-red-300 disabled:bg-red-300"
					><MdiLogout /></button
				>
			</div>
		</div>
		<div class="mx-auto w-full {expanded ? "" : "max-w-6xl"} px-2" style="min-height: 0;">
			<Breadcrumb />
			{@render children()}
			<div class="h-8"></div>
		</div>
    </div>
{:else}
	<div class="absolute h-screen w-screen inset-0 bg-base-100 z-100 flex flex-col transition-opacity transition-normal" out:fade>
		<div class="m-auto flex flex-col gap align-center">
			<span class="loading loading-dots loading-lg mx-auto"></span>
			Warten auf Authentifizierung
		</div>
	</div>
{/if}