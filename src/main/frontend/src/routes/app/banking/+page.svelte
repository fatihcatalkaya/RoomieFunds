<script module lang="ts">
    export const breadcrumbLabel = "Open Banking";
</script>

<script>
	import ErrorAlert from "$lib/components/ErrorAlert.svelte";
    import MdiDelete from "~icons/mdi/delete";
	import type { PageProps } from "./$types";
	import { deleteApiEnablebankingUnfinishedSessionBySessionId, getApiEnablebankingSession, type EnableBankingSession } from "$lib/client";
	import { goto } from "$app/navigation";

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
			realBankingSessionsQuery = getApiEnablebankingSession().then(query => query.error ? [] : query.data!)
		}
	}

	function isExpired(validUntil: string): boolean {
		return new Date(validUntil) < new Date();
	}

	function isExpiringSoon(validUntil: string): boolean {
		const sevenDaysFromNow = new Date();
		sevenDaysFromNow.setDate(sevenDaysFromNow.getDate() + 7);
		return !isExpired(validUntil) && new Date(validUntil) < sevenDaysFromNow;
	}

	function isFinished(session: EnableBankingSession): boolean {
		return !!session.bankAccountIban && !!session.bankAccountUid && session.accountId != null;
	}
</script>

{#if deleteErrorState}
	<ErrorAlert>
		Die Sitzung konnte nicht widerrufen werden!
	</ErrorAlert>
{/if}

<dialog class="modal" bind:this={deleteModal}>
	<div class="modal-box">
		<h3 class="text-lg font-bold">Sitzung {deleteSession?.id} widerrufen?</h3>
		<p class="py-4">Bist du dir sicher, dass du Sitzung {deleteSession?.id} bei {deleteSession?.bankName} widerrufen willst?</p>
		<div class="modal-action">
			<form method="dialog" class="join">
				<button class="btn btn-error join-item" onclick={() => revokeSession(deleteSession?.id!)}>Widerrufen</button>
				<button class="btn join-item">Abbrechen</button>
			</form>
		</div>
	</div>
	<form method="dialog" class="modal-backdrop">
		<button>close</button>
	</form>
</dialog>

<div class="inline-flex items-center w-full my-4 gap-2">
	<h1 class="text-2xl flex-1 font-bold pr-2">
		Banking
	</h1>
    <a href="select-aspsp" title="Hilfe" class="btn btn-primary h-8 py-0 px-2 m-0" onclick={() => newButtonIsLoading = true}>
		{#if newButtonIsLoading}
			<span class="text-primary-content/30">Neues Bankkonto Autorisieren</span>
			<span class="absolute inset-auto z-10 loading loading-spinner loading-sm mx-auto"></span>
		{:else}
			Neues Bankkonto Autorisieren
		{/if}
	</a>
</div>

{#await realBankingSessionsQuery}
	<div class="flex mt-4">
		<span class="loading loading-spinner loading-lg mx-auto"></span>
	</div>
{:then bankingSessions}
	<div class="rounded-box border-base-content/5 bg-base-100 overflow-x-auto border border-slate-300 px-0 mx-0">
		<table class="table table-zebra text-nowrap">
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
								{#if bankingSession.validUntil && isExpired(bankingSession.validUntil)}
									<span class="badge badge-error">Abgelaufen</span>
								{:else}
									<a href="complete/{bankingSession.id}" class="btn btn-warning h-8">Vervollständigen</a>
								{/if}
							{:else}
								{bankingSession.bankAccountIban}
							{/if}
						</td>
						<td>
							<span>{new Date(bankingSession.validUntil!).toLocaleString()}</span>
							{#if bankingSession.validUntil && isExpired(bankingSession.validUntil)}
								<span class="badge badge-error badge-sm ml-1">Abgelaufen</span>
							{:else if bankingSession.validUntil && isExpiringSoon(bankingSession.validUntil)}
								<span class="badge badge-warning badge-sm ml-1">Läuft bald ab</span>
							{/if}
						</td>
						<td>{bankingSession.accountName ?? bankingSession.accountId ?? '–'}</td>
						<td class="text-center flex gap-1 justify-center">
							{#if isFinished(bankingSession) && bankingSession.validUntil && !isExpired(bankingSession.validUntil)}
								<a href="transactions/{bankingSession.id}" class="btn btn-info h-8 px-2" title="Transaktionen anzeigen">Transaktionen</a>
							{/if}
							<button title="Sitzung {bankingSession.id} Löschen!" onclick={() => confirmRevokeSession(bankingSession)} class="btn btn-error h-8 w-8 p-0 m-0 text-lg"><MdiDelete/></button>
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
