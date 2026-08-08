<script module lang="ts">
	export const breadcrumbLabel = 'Gruppen';
</script>

<script lang="ts">
	import MdiPencil from '~icons/mdi/pencil';
	import MdiPlus from '~icons/mdi/plus';
	import MdiSync from '~icons/mdi/sync';
	import type { PageProps } from './$types';
	import { postApiKeycloakSyncFull } from '$lib/client';

	const { data }: PageProps = $props();
	const { groupQuery } = data.streamed;

	let syncLoading = $state(false);
	let syncSuccess = $state(false);
	let syncModal: HTMLDialogElement;

	async function fullSync() {
		syncLoading = true;
		const res = await postApiKeycloakSyncFull();
		syncSuccess = !res.error;
		syncLoading = false;
		syncModal.showModal();
	}
</script>

<dialog class="modal" bind:this={syncModal}>
	<div class="modal-box">
		<h3 class="text-lg font-bold">Keycloak Synchronisation</h3>
		<p class="py-4">
			{#if syncSuccess}
				Vollständige Synchronisation erfolgreich abgeschlossen.
			{:else}
				Fehler bei der Synchronisation!
			{/if}
		</p>
		<div class="modal-action">
			<form method="dialog"><button class="btn">OK</button></form>
		</div>
	</div>
	<form method="dialog" class="modal-backdrop"><button>close</button></form>
</dialog>

<div class="my-4 inline-flex w-full items-center gap-1">
	<h1 class="flex-grow text-2xl font-bold">Gruppen</h1>
	<button
		class="btn btn-warning m-0 h-8 w-8 p-0 text-lg"
		onclick={fullSync}
		disabled={syncLoading}
		title="Vollständige Synchronisation"
	>
		{#if syncLoading}
			<span class="loading loading-spinner loading-sm"></span>
		{:else}
			<MdiSync />
		{/if}
	</button>
	<a
		href="/app/groups/create"
		title="Gruppe Erstellen"
		class="btn btn-success m-0 h-8 w-8 p-0 text-lg"
	>
		<MdiPlus />
	</a>
</div>

{#await groupQuery}
	Loading groups...
{:then groups}
	<div
		class="rounded-box border-base-content/5 bg-base-100 mx-0 overflow-x-auto border border-slate-300 px-0"
	>
		<table class="table-zebra table text-nowrap">
			<thead>
				<tr>
					<th>ID</th>
					<td>Name</td>
					<td>Keycloak Group ID</td>
					<td class="w-6 text-center">Aktion</td>
				</tr>
			</thead>
			<tbody>
				{#each groups! as group}
					<tr>
						<th>{group.id}</th>
						<td>{group.name}</td>
						<td>{group.keycloakGroupId ?? '-'}</td>
						<td class="text-center">
							<a
								href="/app/groups/edit/{group.id}"
								title="Gruppe {group.id} bearbeiten"
								class="btn btn-primary m-0 h-8 w-8 p-0 text-lg"
							>
								<MdiPencil />
							</a>
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>
{:catch error}
	Error while fetching groups!
	<pre>{JSON.stringify(error, null, 2)}</pre>
{/await}
