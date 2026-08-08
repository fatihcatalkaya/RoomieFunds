<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import {
		deleteApiPersonByPersonId,
		patchApiPersonByPersonId,
		getApiPersonByPersonIdGroups,
		postApiPersonByPersonIdSyncKeycloak,
		getApiGroup,
		deleteApiGroupByGroupIdPersonsByPersonId,
		postApiGroupByGroupIdPersons
	} from '$lib/client';
	import MdiDelete from '~icons/mdi/delete';
	import type { PageProps } from './$types';

	const { data }: PageProps = $props();
	let { data: person, error } = data.person;
	if (!person) throw error;

	let firstName = $state(person.firstName);
	let lastName = $state(person.lastName);
	let email = $state(person.email);
	let room = $state(person.room);
	let paysFloorFees = $state(person.paysFloorFees);
	let printOnProductTallyList = $state(person.printOnProductTallyList);
	let emailAccountStatement = $state(person.emailAccountStatement);

	async function updatePerson() {
		let query = await patchApiPersonByPersonId({
			path: {
				personId: person!.id!
			},
			body: {
				firstName,
				lastName,
				room,
				paysFloorFees,
				printOnProductTallyList,
				email,
				emailAccountStatement
			}
		});

		if (query.error) {
			console.error(query.error);
		} else {
			goto('../');
		}

		return false;
	}

	let deleteConfirmModal: HTMLDialogElement;
	let showConflictError = $state(false);

	let groupsPromise = $state(loadGroups());
	let allGroupsPromise = $state(loadAllGroups());
	let selectedGroupId = $state(0);
	let syncLoading = $state(false);
	let syncSuccess = $state(false);
	let syncModal: HTMLDialogElement;

	async function loadGroups() {
		const res = await getApiPersonByPersonIdGroups({ path: { personId: person!.id! } });
		if (res.error) throw res.error;
		return res.data!;
	}

	async function loadAllGroups() {
		const res = await getApiGroup();
		if (res.error) throw res.error;
		return res.data!;
	}

	async function addGroup() {
		if (selectedGroupId === 0) return;
		await postApiGroupByGroupIdPersons({
			path: { groupId: selectedGroupId },
			body: { personId: person!.id! }
		});
		selectedGroupId = 0;
		groupsPromise = loadGroups();
	}

	async function removeGroup(groupId: number) {
		await deleteApiGroupByGroupIdPersonsByPersonId({ path: { groupId, personId: person!.id! } });
		groupsPromise = loadGroups();
	}

	async function syncKeycloak() {
		syncLoading = true;
		const res = await postApiPersonByPersonIdSyncKeycloak({ path: { personId: person!.id! } });
		syncSuccess = !res.error;
		syncLoading = false;
		syncModal.showModal();
	}

	async function deletePerson() {
		deleteConfirmModal.showModal();
	}

	async function reallyDeletePerson() {
		let query = await deleteApiPersonByPersonId({
			path: {
				personId: person!.id!
			}
		});

		if (query.error && query.response.status == 409) {
			showConflictError = true;
		} else {
			goto('../');
		}

		return true;
	}
</script>

<dialog class="modal" bind:this={deleteConfirmModal}>
	<div class="modal-box">
		<h3 class="text-lg font-bold">{firstName} {lastName} löschen</h3>
		<p class="py-4">Bist du dir sicher, dass du Person {person.id} löschen willst?</p>
		<div class="modal-action">
			<form method="dialog" class="join">
				<button class="btn btn-error join-item" onclick={reallyDeletePerson}>Löschen</button>
				<button class="btn join-item">Abbrechen</button>
			</form>
		</div>
	</div>
	<form method="dialog" class="modal-backdrop">
		<button>close</button>
	</form>
</dialog>

{#if showConflictError}
	<div role="alert" class="alert alert-error mt-4">
		<svg
			xmlns="http://www.w3.org/2000/svg"
			class="h-6 w-6 shrink-0 stroke-current"
			fill="none"
			viewBox="0 0 24 24"
		>
			<path
				stroke-linecap="round"
				stroke-linejoin="round"
				stroke-width="2"
				d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"
			/>
		</svg>
		<span
			>Die Person kann nicht gelöscht werden, da mit dem zugehörigen Konto Buchungen verknüpft sind.</span
		>
	</div>
{/if}

<div class="my-4 inline-flex w-full items-center">
	<h1 class="flex-grow text-2xl font-bold">
		Person {page.params.id} Bearbeiten
	</h1>
	<button class="btn btn-error m-0 h-8 w-8 p-0 text-lg" onclick={deletePerson}>
		<MdiDelete />
	</button>
</div>

<form class="mx-auto grid max-w-md grid-cols-1 gap-2">
	<label class="flex w-full items-center">
		<span class="w-1/4">ID</span>
		<input type="text" class="input w-3/4" value={person.id} disabled />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">Vorname</span>
		<input type="text" class="input w-3/4" placeholder="Peter" bind:value={firstName} />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">Nachname</span>
		<input type="text" class="input w-3/4" placeholder="Lustig" bind:value={lastName} />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">Zimmer</span>
		<input type="text" class="input w-3/4" placeholder="R400" bind:value={room} />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">E-Mail</span>
		<input
			type="text"
			class="input w-3/4"
			placeholder="peter@lustig.de"
			minlength="3"
			required
			bind:value={email}
		/>
	</label>
	<div class="flex w-full items-center">
		<span class="w-1/4 text-center"> </span>
		<label class="flex w-3/4 items-center gap-2">
			<input type="checkbox" class="checkbox" bind:checked={paysFloorFees} />
			<span class="flex-grow">Bezahlt Flurbeitrag</span>
		</label>
	</div>
	<div class="flex w-full items-center">
		<span class="w-1/4 text-center"> </span>
		<label class="flex w-3/4 items-center gap-2">
			<input type="checkbox" class="checkbox" bind:checked={printOnProductTallyList} />
			<span class="flex-grow">Darf Getränkeliste</span>
		</label>
	</div>
	<div class="flex w-full items-center">
		<span class="w-1/4 text-center"> </span>
		<label class="flex w-3/4 items-center gap-2">
			<input type="checkbox" class="checkbox" bind:checked={emailAccountStatement} />
			<span class="flex-grow">Erhält Konto-Auszug per E-Mail</span>
		</label>
	</div>
	{#if person.keycloakUserId}
		<label class="mt-2 flex w-full items-center">
			<span class="w-1/4">KC User</span>
			<input type="text" class="input w-3/4" value={person.keycloakUserId} disabled />
		</label>
	{/if}
	<div class="join mt-2 w-full">
		<a href="/app/persons" class="join-item btn btn-warn w-1/2"> Zurück </a>
		<button class="join-item btn btn-success w-1/2" onclick={updatePerson}> Speichern </button>
	</div>
</form>

<div class="mx-auto mt-6 max-w-md">
	<h2 class="mb-2 text-xl font-bold">Gruppen</h2>
	{#await groupsPromise}
		<span class="loading loading-spinner"></span>
	{:then groups}
		<div class="mb-4 flex flex-wrap gap-2">
			{#if person.paysFloorFees}
				<div class="badge badge-info tooltip gap-1" data-tip="Automatisch durch Flurbeitrag">
					floor-members
				</div>
			{/if}
			{#each groups as group}
				<div class="badge badge-outline gap-1">
					{group.name}
					<button class="btn btn-ghost btn-xs p-0" onclick={() => removeGroup(group.id)}>✕</button>
				</div>
			{/each}
		</div>
		{#await allGroupsPromise then allGroups}
			{@const availableGroups = allGroups.filter((g) => !groups.some((pg) => pg.id === g.id))}
			{#if availableGroups.length > 0}
				<div class="flex gap-2">
					<select class="select select-bordered flex-grow" bind:value={selectedGroupId}>
						<option value={0} disabled>Gruppe hinzufügen...</option>
						{#each availableGroups as group}
							<option value={group.id}>{group.name}</option>
						{/each}
					</select>
					<button class="btn btn-primary" onclick={addGroup} disabled={selectedGroupId === 0}
						>+</button
					>
				</div>
			{/if}
		{/await}
	{:catch}
		<p class="text-error">Fehler beim Laden der Gruppen</p>
	{/await}
</div>

<div class="mx-auto mt-6 max-w-md">
	<button class="btn btn-warning w-full" onclick={syncKeycloak} disabled={syncLoading}>
		{#if syncLoading}
			<span class="loading loading-spinner loading-sm"></span>
		{/if}
		Keycloak synchronisieren
	</button>
</div>

<dialog class="modal" bind:this={syncModal}>
	<div class="modal-box">
		<h3 class="text-lg font-bold">Keycloak Sync</h3>
		<p class="py-4">
			{#if syncSuccess}
				Synchronisation erfolgreich.
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
