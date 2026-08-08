<script module lang="ts">
	export const breadcrumbLabel = 'Erstellen';
</script>

<script lang="ts">
	import { goto } from '$app/navigation';

	import { postApiProduct } from '$lib/client';
	import { error } from '@sveltejs/kit';
	import EuroInput from '$lib/components/EuroInput.svelte';

	let name = $state('');
	let price: number | undefined = $state();
	let print = $state(true);

	async function postProduct() {
		const query = await postApiProduct({
			body: { name, price: price!, print }
		});

		if (query.error) {
			console.error(error);
		} else {
			goto('../');
			return false;
		}
	}
</script>

<div class="my-4 inline-flex w-full items-center">
	<h1 class="flex-grow text-2xl font-bold">Produkt Erstellen</h1>
</div>

<form method="dialog" class="mx-auto grid max-w-md grid-cols-1 gap-2">
	<label class="flex w-full items-center">
		<span class="w-1/4">Name</span>
		<input type="text" class="input w-3/4" placeholder="Lecker Bierchen" bind:value={name} />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">Price</span>
		<EuroInput class="input w-3/4" placeholder="1,00" bind:value={price} />
	</label>
	<div class="flex w-full items-center">
		<span class="w-1/4 text-center"> </span>
		<label class="flex w-3/4 items-center gap-2">
			<input type="checkbox" class="checkbox" bind:checked={print} />
			<span class="flex-grow">auf Strichliste drucken</span>
		</label>
	</div>
	<div class="join mt-2 w-full">
		<a href="/app/products" class="join-item btn btn-warn w-1/2"> Zurück </a>
		<input
			class="join-item btn btn-success w-1/2"
			value="Speichern"
			type="submit"
			onclick={postProduct}
		/>
	</div>
</form>
