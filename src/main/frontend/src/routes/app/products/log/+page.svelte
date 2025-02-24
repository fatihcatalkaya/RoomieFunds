<script module>
	export const breadcrumbLabel = 'Protokoll';
</script>

<script lang="ts">
	import { getApiLog } from '$lib/client';
	import LogTable from '$lib/components/LogTable.svelte';
	import { error } from '@sveltejs/kit';

	const logQuery = $derived.by(async () => {
		const query = await getApiLog();

		if (query.error) {
			throw error;
		} else {
			return query.data;
		}
	});
</script>

{#await logQuery}
<LogTable logEntries={[]}/>
{:then logEntries}
	<LogTable logEntries={logEntries!}/>
{:catch error}
	Error while fetching Logs!
	<pre class="mt-4">{JSON.stringify(error, null, 4)}</pre>
{/await}
