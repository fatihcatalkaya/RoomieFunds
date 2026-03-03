<script module lang="ts">
	export const breadcrumbLabel = 'Transaktionen';
</script>

<script lang="ts">
	import { getApiEnablebankingSessionBySessionIdTransactions, type BankTransactionDto, type BankTransactionsResult } from '$lib/client';
	import { formatEuroCents } from '$lib/formatter';
	import ErrorAlert from '$lib/components/ErrorAlert.svelte';
	import type { PageProps } from './$types';

	const { data }: PageProps = $props();

	let dateFrom = $state(data.initialDateFrom);
	let dateTo = $state(data.initialDateTo);
	let transactionsQuery = $state<Promise<BankTransactionsResult>>(data.streamed.transactionsQuery);
	let loading = $state(false);

	async function fetchTransactions() {
		loading = true;
		transactionsQuery = (async () => {
			const query = await getApiEnablebankingSessionBySessionIdTransactions({
				path: { sessionId: data.sessionId },
				query: { dateFrom, dateTo }
			});
			loading = false;
			if (query.error) {
				throw query.error;
			}
			return query.data!;
		})();
	}

	function getCounterparty(tx: BankTransactionDto): string {
		if (tx.creditDebitIndicator === 'CRDT') {
			return tx.debtorName ?? tx.debtorIban ?? '–';
		}
		return tx.creditorName ?? tx.creditorIban ?? '–';
	}

	function getRemittanceInfo(tx: BankTransactionDto): string {
		return tx.remittanceInformation?.join(' ') ?? '–';
	}
</script>

<div class="inline-flex items-center w-full my-4 gap-2">
	<h1 class="text-2xl flex-1 font-bold pr-2">Transaktionen</h1>
	<a href="/app/banking" class="btn btn-sm">Zurück</a>
</div>

<form class="flex flex-wrap items-end gap-4 mb-6" onsubmit={(e) => { e.preventDefault(); fetchTransactions(); }}>
	<fieldset class="fieldset">
		<legend class="fieldset-legend">Von</legend>
		<input type="date" class="input input-bordered" bind:value={dateFrom} />
	</fieldset>
	<fieldset class="fieldset">
		<legend class="fieldset-legend">Bis</legend>
		<input type="date" class="input input-bordered" bind:value={dateTo} />
	</fieldset>
	<button class="btn btn-primary" type="submit" disabled={loading}>
		{#if loading}
			<span class="loading loading-spinner loading-sm"></span>
		{/if}
		Laden
	</button>
</form>

{#await transactionsQuery}
	<div class="flex mt-4">
		<span class="loading loading-spinner loading-lg mx-auto"></span>
	</div>
{:then result}
	{#if result.bankName || result.iban}
		<div class="flex gap-4 mb-4 text-sm text-base-content/70">
			{#if result.bankName}<span><strong>Bank:</strong> {result.bankName}</span>{/if}
			{#if result.iban}<span><strong>IBAN:</strong> {result.iban}</span>{/if}
		</div>
	{/if}

	<div class="rounded-box border-base-content/5 bg-base-100 overflow-x-auto border border-slate-300">
		<table class="table table-zebra text-nowrap">
			<thead>
				<tr>
					<th>Datum</th>
					<th>Betrag</th>
					<th>Auftraggeber/Empfänger</th>
					<th>Verwendungszweck</th>
					<th>Status</th>
				</tr>
			</thead>
			<tbody>
				{#each result.transactions ?? [] as tx}
					<tr>
						<td>{tx.bookingDate ? new Date(tx.bookingDate).toLocaleDateString('de-DE') : '–'}</td>
						<td>
							<span class={tx.creditDebitIndicator === 'CRDT' ? 'text-success font-semibold' : 'text-error font-semibold'}>
								{tx.creditDebitIndicator === 'CRDT' ? '+' : '-'}{formatEuroCents(tx.amountCents ?? 0)}
							</span>
						</td>
						<td>{getCounterparty(tx)}</td>
						<td class="max-w-xs truncate" title={getRemittanceInfo(tx)}>{getRemittanceInfo(tx)}</td>
						<td><span class="badge badge-sm">{tx.status ?? '–'}</span></td>
					</tr>
				{/each}
				{#if !result.transactions || result.transactions.length === 0}
					<tr>
						<td colspan="5" class="text-center text-lg py-8">
							Keine Transaktionen im gewählten Zeitraum gefunden.
						</td>
					</tr>
				{/if}
			</tbody>
		</table>
	</div>

	<div class="mt-2 text-sm text-base-content/60">
		{result.transactions?.length ?? 0} Transaktionen
	</div>
{:catch error}
	<ErrorAlert>Die Transaktionen konnten nicht geladen werden!</ErrorAlert>
	<pre class="mt-4 text-sm">{JSON.stringify(error, null, 2)}</pre>
{/await}
