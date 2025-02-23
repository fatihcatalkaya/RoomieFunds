<script module lang="ts">
	export const breadcrumbLabel = "Transaktionen";
</script>

<script lang="ts">
	import { goto } from "$app/navigation";
	import { onMount } from "svelte";

    onMount(() => goto('../'))
</script>