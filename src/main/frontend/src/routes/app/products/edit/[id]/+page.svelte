<script lang="ts">
	import { page } from "$app/state";

    type Product = {
		id: number;
		name: string;
		price: number;
	}

    let product: Product = $derived({
        id: 1,
        name: "Paulaner Spezi",
        price: 96,
    })
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">
		Produkt {page.params.id} Bearbeiten
	</h1>
</div>

<div class="grid grid-cols-1 gap-2 max-w-md mx-auto">
    <label class="w-full flex items-center">
        <span class="w-1/4">ID</span>
        <input type="text" class="input w-3/4" value="{product.id}" disabled />
    </label>
    <label class="w-full flex items-center">
        <span class="w-1/4">Name</span>
        <input type="text" class="input w-3/4" value="{product.name}" />
    </label>
    <label class="w-full flex items-center">
        <span class="w-1/4">Price</span>
        <input type="number" class="input w-3/4" value="{product.price}" />
    </label>
    <div class="join w-full mt-2">
        <a href="/app/products" class="join-item btn btn-warn w-1/2">
            Zurück
        </a>
        <button class="join-item btn btn-success w-1/2">
            Speichern
        </button>
    </div>
</div>