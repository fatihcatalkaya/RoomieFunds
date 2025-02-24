<script module lang="ts">
    export const breadcrumbLabel = "Erstellen";
</script>

<script lang="ts">
	import { goto } from "$app/navigation";

	import { postApiPerson } from "$lib/client";

    let name = $state("");
	let room = $state("");
	let paysFloorFees = $state(true);
	let printOnProductTallyList = $state(true);

    async function createPerson() {
        let query = await postApiPerson({
            body: { name, room, paysFloorFees, printOnProductTallyList }
        });

        if (query.error) {
            console.log(query.error);
        } else {
            goto("../");
        }

        return true;
    }
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">
		Person Erstellen
	</h1>
</div>

<form class="mx-auto grid max-w-md grid-cols-1 gap-2">
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
		<button class="join-item btn btn-success w-1/2" onclick={createPerson}> Speichern </button>
	</div>
</form>
