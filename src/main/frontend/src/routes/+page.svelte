<script lang="ts">
	import { goto } from '$app/navigation';
	import { getOidc } from '$lib/oidc';
	import { onMount } from 'svelte';

	async function initializeOidc() {
		const oidc = await getOidc();
		if (!oidc.isUserLoggedIn) {
			oidc.login({
				doesCurrentHrefRequiresAuth: false
			});
		} else {
			goto('/app');
		}
	}

	onMount(() => {
		initializeOidc();
	});
</script>
