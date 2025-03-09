<script module lang="ts">
	export const breadcrumbLabel = 'Open Banking';
</script>

<script>
	import ErrorAlert from '$lib/components/ErrorAlert.svelte';
	import MdiDelete from '~icons/mdi/delete';
	import type { PageProps } from './$types';
	import {
		deleteApiEnablebankingUnfinishedSessionBySessionId,
		getApiEnablebankingSession,
		getApiEnablebankingSynchronizeNow,
		type EnableBankingSession
	} from '$lib/client';
	import { goto } from '$app/navigation';

	const { data }: PageProps = $props();
	const { bankingSessionsQuery } = data.streamed;

	let realBankingSessionsQuery = $state(bankingSessionsQuery);
	let newButtonIsLoading = $state(false);
	let deleteErrorState = $state(false);

	let deleteSession: EnableBankingSession | undefined = $state();
	let deleteModal: HTMLDialogElement;

	function confirmRevokeSession(session: EnableBankingSession) {
		deleteSession = session;
		deleteModal!.showModal();
	}

	async function revokeSession(sessionId: number) {
		const query = await deleteApiEnablebankingUnfinishedSessionBySessionId({
			path: { sessionId }
		});

		if (query.error) {
			deleteErrorState = true;
			console.error(query.error);
		} else {
			realBankingSessionsQuery = getApiEnablebankingSession().then((query) =>
				query.error ? [] : query.data!
			);
		}
	}

	async function syncNow() {
		getApiEnablebankingSynchronizeNow().then((response) => alert('scheduled'));
	}
</script>

{#if deleteErrorState}
	<ErrorAlert>Die Sitzung konnte nicht widerrufen werden!</ErrorAlert>
{/if}

<dialog class="modal" bind:this={deleteModal}>
	<div class="modal-box">
		<h3 class="text-lg font-bold">Sitzung {deleteSession?.id} widerrufen?</h3>
		<p class="py-4">
			Bist du dir sicher, dass du Sitzung {deleteSession?.id} bei {deleteSession?.bankName} widerrufen
			willst?
		</p>
		<div class="modal-action">
			<form method="dialog" class="join">
				<button class="btn btn-error join-item" onclick={() => revokeSession(deleteSession?.id!)}
					>Widerrufen</button
				>
				<button class="btn join-item">Abbrechen</button>
			</form>
		</div>
	</div>
	<form method="dialog" class="modal-backdrop">
		<button>close</button>
	</form>
</dialog>

<div class="my-4 inline-flex w-full items-center gap-2">
	<h1 class="flex-1 pr-2 text-2xl font-bold">Banking</h1>
	<button class="btn btn-primary h-8 px-2 py-0" onclick={syncNow}>Jetzt synchronisieren</button>
	<a
		href="select-aspsp"
		title="Hilfe"
		class="btn btn-primary m-0 h-8 px-2 py-0"
		onclick={() => (newButtonIsLoading = true)}
	>
		{#if newButtonIsLoading}
			<span class="text-primary-content/30">Neues Bankkonto Autorisieren</span>
			<span class="loading loading-spinner loading-sm absolute inset-auto z-10 mx-auto"></span>
		{:else}
			Neues Bankkonto Autorisieren
		{/if}
	</a>
</div>

{#await realBankingSessionsQuery}
	<div class="mt-4 flex">
		<span class="loading loading-spinner loading-lg mx-auto"></span>
	</div>
{:then bankingSessions}
	<div
		class="rounded-box border-base-content/5 bg-base-100 mx-0 overflow-x-auto border border-slate-300 px-0"
	>
		<table class="table-zebra table text-nowrap">
			<thead>
				<tr>
					<th>ID</th>
					<td>Bank</td>
					<td>IBAN</td>
					<td>Gültig bis</td>
					<td>Verknüpftes Konto</td>
					<td class="w-6 text-center">Aktion</td>
				</tr>
			</thead>
			<tbody>
				{#each bankingSessions as bankingSession}
					<tr>
						<td>{bankingSession.id}</td>
						<td>{bankingSession.bankName}</td>
						<td>
							{#if !bankingSession.bankAccountIban}
								<a href="complete/{bankingSession.id}" class="btn btn-warning h-8"
									>Vervollständigen</a
								>
							{:else}
								{bankingSession.bankAccountIban}
							{/if}
						</td>
						<td>{new Date(bankingSession.validUntil!).toLocaleString()}</td>
						<td>{bankingSession.accountId}</td>
						<td class="text-center">
							<button
								title="Sitzung {bankingSession.id} Löschen!"
								onclick={() => confirmRevokeSession(bankingSession)}
								class="btn btn-error m-0 h-8 w-8 p-0 text-lg"><MdiDelete /></button
							>
						</td>
					</tr>
				{/each}
				{#if bankingSessions.length == 0}
					<tr>
						<td colspan="6" class="text-center text-lg">
							Aktuell sind keine Bankkonten verknüpft.
						</td>
					</tr>
				{/if}
			</tbody>
		</table>
	</div>
{:catch error}
	<ErrorAlert>Die Online-Banking-Verknüpfungen konnten nicht geladen werden!</ErrorAlert>
	<pre class="mt-4">{JSON.stringify(error, null, 2)}</pre>
{/await}
