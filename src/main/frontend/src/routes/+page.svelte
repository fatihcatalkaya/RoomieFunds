<script lang="ts">
	import { goto } from '$app/navigation';
	import { OidcWrapper } from '$lib/oidc';
	import { onMount } from 'svelte';

	async function initializeOidc() {
		const oidcPromise = OidcWrapper.getInstance().getOidcClient();
		const oidc = await oidcPromise;
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
