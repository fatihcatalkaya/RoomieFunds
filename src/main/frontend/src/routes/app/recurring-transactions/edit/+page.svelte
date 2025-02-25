<script module lang="ts">
    export const breadcrumbLabel = "Bearbeiten";
</script>

<script lang="ts">
	import { goto } from "$app/navigation";
	import { onMount } from "svelte";

    onMount(() => goto('/app/recurring-transactions'));
</script>
