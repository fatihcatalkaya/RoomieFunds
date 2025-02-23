<script module>
	export const breadcrumbLabel = "Protokoll";
</script>

<script lang="ts">
	import { goto } from "$app/navigation";
	import { onMount } from "svelte";

    onMount(() => goto('../'))
</script>