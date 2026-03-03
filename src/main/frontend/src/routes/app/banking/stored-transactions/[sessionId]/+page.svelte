<script module lang="ts">
	export const breadcrumbLabel = 'Gespeicherte Transaktionen';
</script>

<script lang="ts">
	import type { BankTransactionEntity } from '$lib/client';
	import { formatEuroCents } from '$lib/formatter';
	import ErrorAlert from '$lib/components/ErrorAlert.svelte';
	import type { PageProps } from './$types';

	const { data }: PageProps = $props();

	function getCounterparty(tx: BankTransactionEntity): string {
		if (tx.creditDebitIndicator === 'CRDT') {
			return tx.debtorName ?? tx.debtorIban ?? '–';
		}
		return tx.creditorName ?? tx.creditorIban ?? '–';
	}

	function getRemittanceInfo(tx: BankTransactionEntity): string {
		return tx.remittanceInformation?.join(' ') ?? '–';
	}
</script>

<div class="inline-flex items-center w-full my-4 gap-2">
	<h1 class="text-2xl flex-1 font-bold pr-2">Gespeicherte Transaktionen (Sitzung {data.sessionId})</h1>
	<a href="/app/banking" class="btn btn-sm">Zurück</a>
</div>

{#await data.streamed.transactionsQuery}
	<div class="flex mt-4">
		<span class="loading loading-spinner loading-lg mx-auto"></span>
	</div>
{:then transactions}
	<div class="rounded-box border-base-content/5 bg-base-100 overflow-x-auto border border-slate-300">
		<table class="table table-zebra text-nowrap">
			<thead>
				<tr>
					<th>Datum</th>
					<th>Wertstellung</th>
					<th>Betrag</th>
					<th>Auftraggeber/Empfänger</th>
					<th>Verwendungszweck</th>
					<th>Status</th>
					<th>Referenz</th>
				</tr>
			</thead>
			<tbody>
				{#each transactions as tx}
					<tr>
						<td>{tx.bookingDate ? new Date(tx.bookingDate).toLocaleDateString('de-DE') : '–'}</td>
						<td>{tx.valueDate ? new Date(tx.valueDate).toLocaleDateString('de-DE') : '–'}</td>
						<td>
							<span class={tx.creditDebitIndicator === 'CRDT' ? 'text-success font-semibold' : 'text-error font-semibold'}>
								{tx.creditDebitIndicator === 'CRDT' ? '+' : '-'}{formatEuroCents(tx.amountCents ?? 0)}
							</span>
						</td>
						<td>{getCounterparty(tx)}</td>
						<td class="max-w-xs truncate" title={getRemittanceInfo(tx)}>{getRemittanceInfo(tx)}</td>
						<td><span class="badge badge-sm">{tx.status ?? '–'}</span></td>
						<td class="text-xs text-base-content/60">{tx.entryReference ?? tx.transactionId ?? '–'}</td>
					</tr>
				{/each}
				{#if transactions.length === 0}
					<tr>
						<td colspan="7" class="text-center text-lg py-8">
							Keine gespeicherten Transaktionen vorhanden.
						</td>
					</tr>
				{/if}
			</tbody>
		</table>
	</div>

	<div class="mt-2 text-sm text-base-content/60">
		{transactions.length} Transaktionen gespeichert
	</div>
{:catch error}
	<ErrorAlert>Die gespeicherten Transaktionen konnten nicht geladen werden!</ErrorAlert>
	<pre class="mt-4 text-sm">{JSON.stringify(error, null, 2)}</pre>
{/await}
