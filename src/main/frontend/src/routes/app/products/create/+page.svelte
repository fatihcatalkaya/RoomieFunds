<script module lang="ts">
    export const breadcrumbLabel = "Erstellen";
</script>

<script lang="ts">
	import { goto } from "$app/navigation";

	import { postApiProduct } from "$lib/client";
	import { error } from "@sveltejs/kit";

    let name = $state("");
    let price: number | undefined = $state();
    let print = $state(true);
    
    async function postProduct() {
        const query = await postApiProduct({
            body: { name, price: price!, print }
        })

        if (query.error) {
            console.error(error)
        } else {
            goto("../");
            return false;
        }
    }
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">
		Produkt Erstellen
	</h1>
</div>

<form method="dialog" class="grid grid-cols-1 gap-2 max-w-md mx-auto">
    <label class="w-full flex items-center">
        <span class="w-1/4">Name</span>
        <input type="text" class="input w-3/4" placeholder="Lecker Bierchen" bind:value={name} />
    </label>
    <label class="w-full flex items-center">
        <span class="w-1/4">Price</span>
        <input type="number" class="input w-3/4" placeholder="100" bind:value={price} />
    </label>
    <div class="w-full flex items-center">
        <span class="w-1/4 text-center">
        </span>
        <label class="w-3/4 flex items-center gap-2">
            <input type="checkbox" class="checkbox" bind:checked={print}>
            <span class="flex-grow">auf Strichliste drucken</span>
        </label>
    </div>
    <div class="join w-full mt-2">
        <a href="/app/products" class="join-item btn btn-warn w-1/2">
            Zurück
        </a>
        <input class="join-item btn btn-success w-1/2" value="Speichern" type="submit" onclick={postProduct}>
    </div>
</form>