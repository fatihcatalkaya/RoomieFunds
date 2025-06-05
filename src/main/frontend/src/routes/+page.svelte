<script lang="ts">
	import { goto } from '$app/navigation';
	import { oidcClient } from '$lib/oidc';
	import { onMount } from 'svelte';

	onMount(async () => {
		const oidc = oidcClient;
		if (!oidc.isUserLoggedIn) {
			oidc.login({
				doesCurrentHrefRequiresAuth: false
			});
		} else {
			goto('/app');
		}
	});
</script>
