<script module lang="ts">
    export const breadcrumbLabel = "Erstellen";
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">
		Produkt Erstellen
	</h1>
</div>

<div class="grid grid-cols-1 gap-2 max-w-md mx-auto">
    <label class="w-full flex items-center">
        <span class="w-1/4">Name</span>
        <input type="text" class="input w-3/4" placeholder="Lecker Bierchen" />
    </label>
    <label class="w-full flex items-center">
        <span class="w-1/4">Price</span>
        <input type="number" class="input w-3/4" placeholder="0" />
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