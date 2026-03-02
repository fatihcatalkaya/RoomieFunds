<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import {
		patchApiGroupByGroupId,
		deleteApiGroupByGroupId,
		getApiGroupByGroupIdPersons,
		deleteApiGroupByGroupIdPersonsByPersonId,
		postApiGroupByGroupIdPersons,
		getApiPerson
	} from '$lib/client';
	import MdiDelete from '~icons/mdi/delete';
	import type { PageProps } from './$types';

	const { data }: PageProps = $props();
	let { data: group, error } = data.group;
	if (!group) throw error;

	let name = $state(group.name);
	let keycloakGroupId = $state(group.keycloakGroupId ?? '');

	let personsPromise = $state(loadPersons());
	let allPersonsPromise = $state(loadAllPersons());
	let selectedPersonId = $state(0);

	async function loadPersons() {
		const res = await getApiGroupByGroupIdPersons({ path: { groupId: group!.id! } });
		if (res.error) throw res.error;
		return res.data!;
	}

	async function loadAllPersons() {
		const res = await getApiPerson();
		if (res.error) throw res.error;
		return res.data!;
	}

	async function updateGroup() {
		let query = await patchApiGroupByGroupId({
			path: { groupId: group!.id! },
			body: { name, keycloakGroupId: keycloakGroupId || undefined }
		});
		if (query.error) {
			console.error(query.error);
		} else {
			goto('../');
		}
		return false;
	}

	let deleteConfirmModal: HTMLDialogElement;

	async function deleteGroup() {
		deleteConfirmModal.showModal();
	}

	async function reallyDeleteGroup() {
		let query = await deleteApiGroupByGroupId({ path: { groupId: group!.id! } });
		if (!query.error) {
			goto('../');
		}
		return true;
	}

	async function addPerson() {
		if (selectedPersonId === 0) return;
		await postApiGroupByGroupIdPersons({
			path: { groupId: group!.id! },
			body: { personId: selectedPersonId }
		});
		selectedPersonId = 0;
		personsPromise = loadPersons();
	}

	async function removePerson(personId: number) {
		await deleteApiGroupByGroupIdPersonsByPersonId({
			path: { groupId: group!.id!, personId }
		});
		personsPromise = loadPersons();
	}
</script>

<dialog class="modal" bind:this={deleteConfirmModal}>
	<div class="modal-box">
		<h3 class="text-lg font-bold">{name} löschen</h3>
		<p class="py-4">Bist du dir sicher, dass du Gruppe {group.id} löschen willst?</p>
		<div class="modal-action">
			<form method="dialog" class="join">
				<button class="btn btn-error join-item" onclick={reallyDeleteGroup}>Löschen</button>
				<button class="btn join-item">Abbrechen</button>
			</form>
		</div>
	</div>
	<form method="dialog" class="modal-backdrop"><button>close</button></form>
</dialog>

<div class="my-4 inline-flex w-full items-center">
	<h1 class="flex-grow text-2xl font-bold">Gruppe {page.params.id} Bearbeiten</h1>
	<button class="btn btn-error h-8 w-8 p-0 m-0 text-lg" onclick={deleteGroup}>
		<MdiDelete />
	</button>
</div>

<form class="mx-auto grid max-w-md grid-cols-1 gap-2">
	<label class="flex w-full items-center">
		<span class="w-1/4">ID</span>
		<input type="text" class="input w-3/4" value={group.id} disabled />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">Name</span>
		<input type="text" class="input w-3/4" placeholder="floor-members" bind:value={name} />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">KC Group ID</span>
		<input type="text" class="input w-3/4" placeholder="(optional)" bind:value={keycloakGroupId} />
	</label>
	<div class="join mt-2 w-full">
		<a href="/app/groups" class="join-item btn btn-warn w-1/2">Zurück</a>
		<button class="join-item btn btn-success w-1/2" onclick={updateGroup}>Speichern</button>
	</div>
</form>

<div class="mx-auto max-w-md mt-6">
	<h2 class="text-xl font-bold mb-2">Personen in Gruppe</h2>
	{#await personsPromise}
		<span class="loading loading-spinner"></span>
	{:then persons}
		<div class="rounded-box border-base-content/5 bg-base-100 overflow-x-auto border border-slate-300">
			<table class="table table-zebra text-nowrap">
				<thead>
					<tr>
						<th>ID</th>
						<td>Vorname</td>
						<td>Nachname</td>
						<td>Zimmer</td>
						<td class="w-6"></td>
					</tr>
				</thead>
				<tbody>
					{#each persons as p}
						<tr>
							<th>{p.id}</th>
							<td>{p.firstName}</td>
							<td>{p.lastName}</td>
							<td>{p.room}</td>
							<td>
								<button class="btn btn-error btn-xs" onclick={() => removePerson(p.id)}>✕</button>
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
		{#await allPersonsPromise then allPersons}
			{@const availablePersons = allPersons.filter(p => !persons.some(pp => pp.id === p.id))}
			{#if availablePersons.length > 0}
				<div class="flex gap-2 mt-2">
					<select class="select select-bordered flex-grow" bind:value={selectedPersonId}>
						<option value={0} disabled>Person hinzufügen...</option>
						{#each availablePersons as p}
							<option value={p.id}>{p.firstName} {p.lastName} ({p.room})</option>
						{/each}
					</select>
					<button class="btn btn-primary" onclick={addPerson} disabled={selectedPersonId === 0}>+</button>
				</div>
			{/if}
		{/await}
	{:catch}
		<p class="text-error">Fehler beim Laden der Personen</p>
	{/await}
</div>
