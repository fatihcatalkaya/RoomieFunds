<script module lang="ts">
    export const breadcrumbLabel = "Neu";
</script>

<script lang="ts">
	import { goto } from "$app/navigation";

	import { postApiPerson, type CreatePersonDto } from "$lib/client";

	let newPerson: CreatePersonDto = $state({
		name: "",
		room: "",
		paysFloorFees: true,
		printOnProductTallyList: true,
	})

    async function createPerson() {
        let query = await postApiPerson({
            body: {...newPerson}
        });

        if (query.error) {
            console.error(query.error);
        } else {
            goto("../");
        }

        return false;
    }
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">
		Neue Person
	</h1>
</div>

<form class="mx-auto grid max-w-md grid-cols-1 gap-2" onsubmit={createPerson}>
	<label class="flex w-full items-center">
		<span class="w-1/4">Name</span>
		<input type="text" class="input w-3/4" placeholder="Peter Lustig" minlength=1 required bind:value={newPerson.name} />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">Zimmer</span>
		<input type="text" class="input w-3/4" placeholder="R400" minlength=3 required bind:value={newPerson.room} />
	</label>
	<div class="flex w-full items-center">
		<span class="w-1/4 text-center"> </span>
		<label class="flex w-3/4 items-center gap-2">
			<input type="checkbox" class="checkbox" bind:checked={newPerson.paysFloorFees} />
			<span class="flex-grow">Bezahlt Flurbeitrag</span>
		</label>
	</div>
	<div class="flex w-full items-center">
		<span class="w-1/4 text-center"> </span>
		<label class="flex w-3/4 items-center gap-2">
			<input type="checkbox" class="checkbox" bind:checked={newPerson.printOnProductTallyList} />
			<span class="flex-grow">Darf Getränkeliste</span>
		</label>
	</div>
	<div class="mt-2 join grid grid-cols-2">
		<button class="btn order-1 btn-success join-item">Speichern</button>
		<a href="/app/persons" class="btn order-0 join-item">Zurück</a>
	</div>
</form>
