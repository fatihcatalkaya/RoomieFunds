<script module lang="ts">
    export const breadcrumbLabel = "Open Banking";
</script>

<script>
	import ErrorAlert from "$lib/components/ErrorAlert.svelte";
	import { formatEuroCents } from "$lib/formatter";
    import MdiDelete from "~icons/mdi/delete";
    import MdiSync from "~icons/mdi/sync";
    import MdiAlert from "~icons/mdi/alert";
    import MdiCheckCircle from "~icons/mdi/check-circle";

	import type { PageProps } from "./$types";
	import {
		deleteApiEnablebankingUnfinishedSessionBySessionId,
		getApiEnablebankingSession,
		getApiEnablebankingSyncStatus,
		postApiEnablebankingSync,
		postApiEnablebankingSyncBySessionId,
		type EnableBankingSession,
		type SessionSyncStatus,
		type SyncResult
	} from "$lib/client";
	import { goto } from "$app/navigation";

	const { data }: PageProps = $props();
	const { bankingSessionsQuery, syncStatusQuery } = data.streamed;

	let realBankingSessionsQuery = $state(bankingSessionsQuery);
	let realSyncStatusQuery = $state(syncStatusQuery);
	let newButtonIsLoading = $state(false);
	let deleteErrorState = $state(false);

	let deleteSession: EnableBankingSession | undefined = $state();
	let deleteModal: HTMLDialogElement;

	// Sync state
	let syncAllLoading = $state(false);
	let syncLoadingSessionIds = $state(new Set<number>());
	let syncResultsMap = $state(new Map<number, SyncResult>());
	let showSyncBanner = $state(false);

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

	function getSyncStatusForSession(sessionId: number | undefined, statuses: SessionSyncStatus[]): SessionSyncStatus | undefined {
		if (sessionId == null) return undefined;
		return statuses.find(s => s.sessionId === sessionId);
	}

	function formatSyncStatus(status: string | undefined): { label: string; badge: string } {
		switch (status) {
			case 'SUCCESS': return { label: 'OK', badge: 'badge-success' };
			case 'FAILED': return { label: 'Fehler', badge: 'badge-error' };
			case 'AUTH_REQUIRED': return { label: 'Auth. erforderlich', badge: 'badge-warning' };
			case 'EXPIRED': return { label: 'Abgelaufen', badge: 'badge-error' };
			default: return { label: '–', badge: 'badge-ghost' };
		}
	}

	async function refreshSyncStatuses() {
		realSyncStatusQuery = getApiEnablebankingSyncStatus().then(query => query.error ? [] : query.data!);
	}

	async function syncAll() {
		syncAllLoading = true;
		syncResultsMap = new Map();
		const query = await postApiEnablebankingSync();
		syncAllLoading = false;

		if (query.error) {
			console.error(query.error);
		} else {
			for (const result of query.data!) {
				if (result.sessionId != null) {
					syncResultsMap.set(result.sessionId, result);
				}
			}
			syncResultsMap = new Map(syncResultsMap);
			showSyncBanner = true;
		}
		await refreshSyncStatuses();
	}

	async function syncSession(sessionId: number) {
		syncLoadingSessionIds.add(sessionId);
		syncLoadingSessionIds = new Set(syncLoadingSessionIds);

		const query = await postApiEnablebankingSyncBySessionId({ path: { sessionId } });

		syncLoadingSessionIds.delete(sessionId);
		syncLoadingSessionIds = new Set(syncLoadingSessionIds);

		if (query.error) {
			console.error(query.error);
		} else {
			syncResultsMap.set(sessionId, query.data!);
			syncResultsMap = new Map(syncResultsMap);
			showSyncBanner = true;
		}
		await refreshSyncStatuses();
	}

	function dismissSyncBanner() {
		showSyncBanner = false;
		syncResultsMap = new Map();
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
	<button
		class="btn btn-secondary h-8 py-0 px-2 m-0"
		onclick={() => syncAll()}
		disabled={syncAllLoading}
	>
		{#if syncAllLoading}
			<span class="loading loading-spinner loading-sm"></span>
		{:else}
			<MdiSync />
		{/if}
		Alle synchronisieren
	</button>
    <a href="select-aspsp" title="Hilfe" class="btn btn-primary h-8 py-0 px-2 m-0" onclick={() => newButtonIsLoading = true}>
		{#if newButtonIsLoading}
			<span class="text-primary-content/30">Neues Bankkonto Autorisieren</span>
			<span class="absolute inset-auto z-10 loading loading-spinner loading-sm mx-auto"></span>
		{:else}
			Neues Bankkonto Autorisieren
		{/if}
	</a>
</div>

{#if showSyncBanner && syncResultsMap.size > 0}
	<div class="alert mb-4 flex">
		<div class="flex-1">
			<p class="font-semibold">Sync-Ergebnisse</p>
			<ul class="text-sm mt-1">
				{#each [...syncResultsMap.entries()] as [sessionId, result]}
					<li>
						Sitzung {sessionId}:
						{result.transactionsFetched ?? 0} abgerufen, {result.transactionsInserted ?? 0} eingefügt, {result.transactionsDeleted ?? 0} ersetzt
						<span class="badge badge-sm ml-1 {formatSyncStatus(result.status).badge}">{formatSyncStatus(result.status).label}</span>
						{#if result.errorMessage}
							<span class="text-error text-xs ml-1">({result.errorMessage})</span>
						{/if}
					</li>
				{/each}
			</ul>
		</div>
		<button class="btn btn-info btn-sm" onclick={dismissSyncBanner}>Schließen</button>
	</div>
{/if}

{#await realBankingSessionsQuery}
	<div class="flex mt-4">
		<span class="loading loading-spinner loading-lg mx-auto"></span>
	</div>
{:then bankingSessions}
	{#await realSyncStatusQuery then syncStatuses}
		<div class="rounded-box border-base-content/5 bg-base-100 overflow-x-auto border border-slate-300 px-0 mx-0">
			<table class="table table-zebra text-nowrap">
				<thead>
					<tr>
						<th>ID</th>
						<td>Bank</td>
						<td>IBAN</td>
						<td>Gültig bis</td>
						<td>Verknüpftes Konto</td>
						<td>Letzter Sync</td>
						<td>Status</td>
						<td>Saldo</td>
						<td class="w-6 text-center">Aktion</td>
					</tr>
				</thead>
				<tbody>
					{#each bankingSessions as bankingSession}
						{@const syncStatus = getSyncStatusForSession(bankingSession.id, syncStatuses)}
						{@const statusInfo = formatSyncStatus(syncStatus?.lastSyncStatus)}
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
							<td>
								{#if syncStatus?.lastSyncedAt}
									{new Date(syncStatus.lastSyncedAt).toLocaleString('de-DE')}
								{:else}
									Nie
								{/if}
							</td>
							<td>
								<span class="badge badge-sm {statusInfo.badge}">{statusInfo.label}</span>
							</td>
							<td>
								{#if syncStatus?.apiBalanceCents != null}
									<span class="inline-flex items-center gap-1">
										{formatEuroCents(syncStatus.apiBalanceCents)}
										{#if syncStatus.balanceMatch === true}
											<span class="text-success" title="Saldo stimmt überein"><MdiCheckCircle /></span>
										{:else if syncStatus.balanceMatch === false}
											<span class="text-warning cursor-help" title="Bank: {formatEuroCents(syncStatus.apiBalanceCents)} / Berechnet: {formatEuroCents(syncStatus.computedBalanceCents ?? 0)}"><MdiAlert /></span>
										{/if}
									</span>
								{:else}
									–
								{/if}
							</td>
							<td class="text-center flex gap-1 justify-center">
								{#if isFinished(bankingSession) && bankingSession.validUntil && !isExpired(bankingSession.validUntil)}
									<button
										title="Transaktionen synchronisieren"
										class="btn btn-info h-8 w-8 p-0"
										onclick={() => syncSession(bankingSession.id!)}
										disabled={syncLoadingSessionIds.has(bankingSession.id!)}
									>
										{#if syncLoadingSessionIds.has(bankingSession.id!)}
											<span class="loading loading-spinner loading-sm"></span>
										{:else}
											<MdiSync />
										{/if}
									</button>
									<a href="stored-transactions/{bankingSession.id}" class="btn btn-info h-8 px-2" title="Transaktionen anzeigen">Transaktionen</a>
								{/if}
								{#if syncStatus?.lastSyncStatus === 'AUTH_REQUIRED'}
									<a href="select-aspsp" class="btn btn-warning h-8 px-2">Erneut verbinden</a>
								{/if}
								<button title="Sitzung {bankingSession.id} Löschen!" onclick={() => confirmRevokeSession(bankingSession)} class="btn btn-error h-8 w-8 p-0 m-0 text-lg"><MdiDelete/></button>
							</td>
						</tr>
					{/each}
					{#if bankingSessions.length == 0}
						<tr>
							<td colspan="9" class="text-center text-lg">
								Aktuell sind keine Bankkonten verknüpft.
							</td>
						</tr>
					{/if}
				</tbody>
			</table>
		</div>
	{/await}

{:catch error}
	<ErrorAlert>Die Online-Banking-Verknüpfungen konnten nicht geladen werden!</ErrorAlert>
	<pre class="mt-4">{JSON.stringify(error, null, 2)}</pre>
{/await}
