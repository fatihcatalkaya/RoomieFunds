<script module lang="ts">
	export const breadcrumbLabel = 'Erstellen';
</script>

<script lang="ts">
	import { goto } from '$app/navigation';
	import {
		getApiAccount,
		postApiRecurringTransaction,
		type CreateRecurringTransactionDto
	} from '$lib/client';
	import EuroInput from '$lib/components/EuroInput.svelte';

	let accountsQuery = $derived.by(async () => {
		const accountsQuery = await getApiAccount();

		if (accountsQuery.error) {
			throw accountsQuery.error;
		} else {
			return accountsQuery.data;
		}
	});

	let formData: CreateRecurringTransactionDto = $state({
		name: '',
		valueDayOfMonth: 1,
		transactionDescription: ''
	});

	async function createRecurringTransaction() {
		let query = await postApiRecurringTransaction({
			body: { ...formData }
		});

		if (query.error) {
			console.error(query.error);
		} else {
			goto('/app/recurring-transactions');
		}

		return false;
	}
</script>

<div class="my-4 inline-flex w-full items-center">
	<h1 class="flex-grow text-2xl font-bold">Neuer Dauerauftrag</h1>
</div>

<form method="dialog" class="grid grid-cols-1 gap-2" onsubmit={createRecurringTransaction}>
	<label class="flex w-full items-center">
		<span class="w-1/4">Name</span>
		<input type="text" class="input w-3/4" bind:value={formData.name} required />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">Buchen Von</span>
		<select class="select w-3/4" bind:value={formData.sourceAccountId} required>
			{#await accountsQuery}
				<option value="" disabled>Loading accounts...</option>
			{:then accounts}
				{#each accounts! as account}
					<option value={account.id}>{account.name}</option>
				{/each}
			{:catch}
				<option value="" disabled>Error while fetching accounts</option>
			{/await}
		</select>
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">Buchen Nach</span>
		<select class="select w-3/4" bind:value={formData.targetAccountId} required>
			{#await accountsQuery}
				<option value="" disabled>Loading accounts...</option>
			{:then accounts}
				{#each accounts! as account}
					<option value={account.id}>{account.name}</option>
				{/each}
			{:catch}
				<option value="" disabled>Error while fetching accounts</option>
			{/await}
		</select>
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">Betrag</span>
		<EuroInput class="input w-3/4" bind:value={formData.amount} required />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4"
			><span class="after:content-['Tag'] md:after:content-['Wertstellungstag']"></span> des Monats</span
		>
		<input
			type="number"
			class="input w-3/4"
			bind:value={formData.valueDayOfMonth}
			min="1"
			max="31"
			step="1"
			required
		/>
	</label>
	<label class="flex w-full items-center">
		<span
			class="w-1/4 after:content-['Beschr.'] md:after:content-['Beschreibung'] lg:after:content-['Transaktionsbeschreibung']"
		></span>
		<input type="text" class="input w-3/4" bind:value={formData.transactionDescription} required />
	</label>
	<div class="join mt-2 grid grid-cols-2">
		<a href="/app/recurring-transactions" class="join-item btn btn-warn grid-order-1"> Zurück </a>
		<button class="join-item btn btn-success grid-order-0">Speichern</button>
	</div>
</form>
