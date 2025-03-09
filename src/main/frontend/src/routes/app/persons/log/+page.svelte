<script module>
	export const breadcrumbLabel = 'Protokoll';
</script>

<script lang="ts">
	import LogTable from '$lib/components/LogTable.svelte';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
	let { logQuery } = data.streamed
</script>

{#await logQuery}
<LogTable logEntries={[]}/>
{:then logEntries}
	<LogTable logEntries={logEntries!}/>
{:catch error}
	Error while fetching Logs!
	<pre class="mt-4">{JSON.stringify(error, null, 4)}</pre>
{/await}
