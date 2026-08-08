<script module lang="ts">
	export const breadcrumbLabel = 'Striche zählen';
</script>

<script lang="ts">
	import { goto } from '$app/navigation';

	import { getApiPerson, postApiTransaction, type Person, type Product } from '$lib/client';
	import { formatEuroCents } from '$lib/formatter';
	import { error } from '@sveltejs/kit';
	import type { PageProps } from './$types';

	const { data }: PageProps = $props();
	const { getraenkekontoQuery, productsQuery } = data;

	let products = productsQuery.data!;
	let mainAccount = getraenkekontoQuery.data!;

	interface TallyEntry {
		product: Product;
		count: number;
	}

	// Use an array to preserve API order, with a Map for quick lookups by ID
	let tallyData: TallyEntry[] = $state(products.map((product) => ({ product, count: 0 })));
	let tallyCountById = $derived(
		new Map(tallyData.map((entry, index) => [entry.product.id!, index]))
	);
	let sum = $derived(tallyData.reduce((acc, entry) => acc + entry.count * entry.product.price!, 0));

	let personQuery = $derived.by(async () => {
		const query = await getApiPerson();

		if (query.error) {
			throw error;
		} else {
			return query.data
				?.filter((person) => person.printOnProductTallyList)
				.sort((a, b) => (a.room ?? '').localeCompare(b.room ?? '', undefined, { numeric: true }));
		}
	});

	let selectedPerson: Person | undefined = $state();
	let confirmModal: HTMLDialogElement;
	let errorAlert: HTMLDivElement;

	async function submitTransaction() {
		const query = await postApiTransaction({
			body: {
				amount: sum,
				description: 'Strichlistenzählung',
				sourceAccountId: selectedPerson?.accountId,
				targetAccountId: mainAccount.id,
				valueDate: new Date(Date.now()).toISOString().substring(0, 10)
			}
		});

		if (query.error) {
			console.error(error);
			errorAlert.hidden = false;
		} else {
			goto('../');
		}
	}
</script>

<div class="my-4 inline-flex w-full items-center">
	<h1 class="flex-grow text-2xl font-bold">Strichliste zählen</h1>
</div>

<dialog class="modal" bind:this={confirmModal}>
	<div class="modal-box">
		<h3 class="text-lg font-bold">Buchung für {selectedPerson?.name} eintragen?</h3>
		<p class="py-4">
			{selectedPerson?.name} bekommt {formatEuroCents(sum)} von seinem Konto abgebucht.
		</p>
		<div class="modal-action">
			<form method="dialog" class="join">
				<button class="btn join-item">Abbrechen</button>
				<button class="btn btn-success join-item" onclick={submitTransaction}>Eintragen</button>
			</form>
		</div>
	</div>
	<form method="dialog" class="modal-backdrop">
		<button>close</button>
	</form>
</dialog>

<div role="alert" class="alert alert-error mb-4" bind:this={errorAlert} hidden>
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
	<span>Die Buchung konnte nicht eingetragen werden!</span>
</div>

<form class="mx-auto max-w-md" onsubmit={() => confirmModal.showModal()}>
	<label class="flex items-center gap-2">
		<span class="flex-1">Person</span>
		<select class="select flex-2" bind:value={selectedPerson} required>
			{#await personQuery}
				<option value="" disabled>Loading persons...</option>
			{:then personList}
				{#each personList! as person (person.id!)}
					<option value={person}>{person.firstName} {person.lastName} {person.room}</option>
				{/each}
			{:catch}
				<option value="" disabled>Error fetching persons</option>
			{/await}
		</select>
	</label>
	{#each tallyData as tally, index (index)}
		<label class="mt-2 flex items-center gap-2">
			<span class="flex-4">{tally.product.name}</span>
			<span class="flex-1 text-right">{formatEuroCents(tally.product.price!)}</span>
			x
			<input
				type="number"
				class="input flex-2"
				bind:value={tallyData[index].count}
				min="0"
				step="1"
			/>
		</label>
	{/each}
	<hr class="text-base-300 mt-4" />
	<div class="mt-4 flex items-center gap-2 pr-4 font-bold">
		<span class="flex-1">Summe</span>
		<span class="text-right">{formatEuroCents(sum)}</span>
	</div>
	<div class="join mt-4 grid grid-cols-2">
		<button class="btn btn-primary join-item order-1">Bestätigen</button>
		<a class="btn join-item order-0" href="../">Abbrechen</a>
	</div>
</form>
