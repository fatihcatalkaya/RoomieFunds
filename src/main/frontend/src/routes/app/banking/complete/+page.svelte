<script module lang="ts">
	export const breadcrumbLabel = 'Sitzung Vervollständigen';
</script>

<script>
	import { goto } from '$app/navigation';

	import { onMount } from 'svelte';

	onMount(() => goto('../'));
</script>
