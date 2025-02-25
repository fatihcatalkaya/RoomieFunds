<script module lang="ts">
    export const breadcrumbLabel = "Daueraufträge";
</script>

<script lang="ts">
	import MdiPencil from '~icons/mdi/pencil';
	import MdiBank from '~icons/mdi/bank';
	import MdiPlus from '~icons/mdi/plus';
	import MdiCheck from '~icons/mdi/check-bold';
	import MdiClose from '~icons/mdi/close-bold';
    import { getApiRecurringTransaction } from "$lib/client";
	import { formatEuroCents } from '$lib/formatter';
	import Dingsbums from '$lib/components/dingsbums.svelte';

    let recurringQuery = $derived.by(async () => {
		let query = await getApiRecurringTransaction();

		if (query.error) {
			throw query.error;
		} else {
			return query.data;
		}
	});
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">
		Daueraufträge
	</h1>
	<a href="/app/recurring-transactions/create" title="Dauerauftrag Erstellen" class="btn btn-success h-8 w-8 p-0 m-0 text-lg">
		<MdiPlus/>
	</a>
</div>

{#await recurringQuery}
	Loading persons...
{:then recurringTransactions}
	<div class="rounded-box border-base-content/5 bg-base-100 overflow-x-auto border border-slate-300 px-0 mx-0">
		<table class="table table-zebra text-nowrap">
			<thead>
				<tr>
					<th>ID</th>
					<td>Name</td>
					<td>Buchen Von</td>
					<td>Buchen Nach</td>
					<td>Betrag</td>
					<td>Wertstellungstag</td>
                    <td>Transaktionsbeschreibung</td>
					<td class="w-6 text-center">Aktion</td>
				</tr>
			</thead>
			<tbody>
				{#each recurringTransactions! as recurringTransaction}
					<tr>
						<td>{recurringTransaction.id}</td>
						<td>{recurringTransaction.name}</td>
						<td>
                            {#each recurringTransaction.sourceAccountName!.split(":") as part, i}
                                {part}
                                {#if i < recurringTransaction.sourceAccountName!.split(":").length - 1}
                                    <Dingsbums/>
                                {/if}
                            {/each}
                        </td>
						<td>
                            {#each recurringTransaction.targetAccountName!.split(":") as part, i}
                                {part}
                                {#if i < recurringTransaction.targetAccountName!.split(":").length - 1}
                                    <Dingsbums/>
                                {/if}
                            {/each}
                        </td>
						<td>{formatEuroCents(recurringTransaction.amount!)}</td>
                        <td>{recurringTransaction.valueDayOfMonth}</td>
                        <td>{recurringTransaction.transactionDescription}</td>
						<td class="text-center">
							<a href="/app/recurring-transactions/edit/{recurringTransaction.id}" title="Dauerauftrag {recurringTransaction.id} bearbeiten" class="btn btn-primary h-8 w-8 p-0 m-0 text-lg">
								<MdiPencil />
							</a>
						</td>
					</tr>
				{/each}
                {#if recurringTransactions?.length == 0}
                    <tr>
                        <td colspan="8" class="text-center text-lg">
                            Es gibt keine Daueraufträge.
                        </td>
                    </tr>
                {/if}
			</tbody>
		</table>
	</div>	
{:catch error}
	Error while fetching persons!
	<pre>{JSON.stringify(error, null, 2)}</pre>
{/await}
