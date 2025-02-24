<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { deleteApiPersonByPersonId, patchApiPersonByPersonId } from '$lib/client';
    import MdiDelete from '~icons/mdi/delete';

	const { data } = $props();

	let person = data.data!;
	let error = data.error;

	let name = $state(person.name);
	let room = $state(person.room);
	let paysFloorFees = $state(person.paysFloorFees);
	let printOnProductTallyList = $state(person.printOnProductTallyList);

	async function updatePerson() {
		let query = await patchApiPersonByPersonId({
			path: {
				personId: person.id!
			},
			body: { name, room, paysFloorFees, printOnProductTallyList }
		});

		if (query.error) {
			console.error(query.error);
		} else {
			goto('../');
		}

		return false;
	}

    let modalElement: HTMLDialogElement;
    let showConflictError = $state(false);

	async function deletePerson() {
        modalElement.showModal();
    }

    async function reallyDeletePerson() {
        let query = await deleteApiPersonByPersonId({
            path: {
                personId: person.id!,
            }
        });

        if (query.error && query.response.status == 409) {
            showConflictError = true;
        } else {
            goto("../")
        }

        return true
    }

	// TODO: Account dingsbums
</script>

<dialog class="modal" bind:this={modalElement}>
	<div class="modal-box">
		<h3 class="text-lg font-bold">{name} löschen</h3>
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
        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 shrink-0 stroke-current" fill="none" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>Die Person kann nicht gelöscht werden, da mit dem zugehörigen Konto Buchungen verknüpft sind.</span>
    </div>
{/if}

<div class="my-4 inline-flex w-full items-center">
	<h1 class="flex-grow text-2xl font-bold">
		Person {page.params.id} Bearbeiten
	</h1>
    <button class="btn btn-error h-8 w-8 p-0 m-0 text-lg" onclick={deletePerson}>
        <MdiDelete/>
    </button>
</div>

<form class="mx-auto grid max-w-md grid-cols-1 gap-2">
	<label class="flex w-full items-center">
		<span class="w-1/4">ID</span>
		<input type="text" class="input w-3/4" value={person.id} disabled />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">Name</span>
		<input type="text" class="input w-3/4" placeholder="Peter Lustig" bind:value={name} />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">Zimmer</span>
		<input type="text" class="input w-3/4" placeholder="R400" bind:value={room} />
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
	<div class="join mt-2 w-full">
		<a href="/app/persons" class="join-item btn btn-warn w-1/2"> Zurück </a>
		<button class="join-item btn btn-success w-1/2" onclick={updatePerson}> Speichern </button>
	</div>
</form>
