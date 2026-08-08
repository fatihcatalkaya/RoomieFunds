<script lang="ts">
	import {
		getApiAccount,
		getApiTransactionByTransactionIdReceipt,
		postApiTransaction,
		postApiTransactionByTransactionIdReceipt,
		type Account
	} from '$lib/client';
	import { error } from '@sveltejs/kit';
	import { todayAsIsoDate } from '$lib/formatter';
	import MdiChequebookRight from '~icons/mdi/chequebook-arrow-left';
	import EuroInput from '$lib/components/EuroInput.svelte';
	import ReceiptFileInput from '$lib/components/ReceiptFileInput.svelte';
	import { bookingFor } from '$lib/transactionAmount';

	const accountList = $derived.by(async () => {
		const accountQuery = await getApiAccount();

		if (accountQuery.error) {
			throw error;
		} else {
			return accountQuery.data!;
		}
	});

	type TransactionInsertProps = {
		account: Account;
		refreshTransactions: () => void;
	};

	let { account, refreshTransactions }: TransactionInsertProps = $props();

	let date: string = $state(todayAsIsoDate());
	let description: string = $state('');
	let bookAccountId: number | undefined = $state();
	let amount: number | null = $state(null);
	let files: FileList | undefined = $state();

	async function submitTransaction(event: SubmitEvent) {
		event.preventDefault();

		const counterAccount = (await accountList).find((entry) => entry.id === bookAccountId);
		if (!counterAccount || amount == null || amount === 0) {
			return;
		}

		const query = await postApiTransaction({
			body: {
				valueDate: new Date(date).toISOString().substring(0, 10),
				description,
				...bookingFor(amount, account, counterAccount)
			}
		});

		if (query.error) {
			console.error(error);
		} else if (files && files.length > 0) {
			const receiptQuery = await postApiTransactionByTransactionIdReceipt({
				path: {
					transactionId: query.data?.id!
				},
				body: {
					receipt: files[0]
				}
			});

			if (receiptQuery.error) {
				console.error(error);
			}
		}

		refreshTransactions();
	}
</script>

<tr class="bg-base-200">
	<td>
		<input form="transaction-new-form" bind:value={date} type="date" class="input" required />
	</td>
	<td>
		<input
			form="transaction-new-form"
			bind:value={description}
			type="text"
			class="input"
			placeholder="Beschreibung"
		/>
	</td>
	<td>
		<select form="transaction-new-form" bind:value={bookAccountId} class="select">
			{#await accountList}
				<option value="" disabled>Loading...</option>
			{:then accountList}
				{#each accountList as accountEntry}
					{#if accountEntry.id !== account.id}
						<option value={accountEntry.id}>{accountEntry.name}</option>
					{/if}
				{/each}
			{:catch error}
				<option value="" disabled>Error fetching accounts!</option>
			{/await}
		</select>
	</td>
	<td>
		<ReceiptFileInput bind:files />
	</td>
	<td>
		<EuroInput
			class="input min-w-20"
			form="transaction-new-form"
			bind:value={amount}
			allowNegative
		/>
	</td>
	<td></td>
	<td>
		<form id="transaction-new-form" onsubmit={submitTransaction}>
			<button title="Buchen" class="btn btn-success m-0 h-8 w-17 p-0 text-lg"
				><MdiChequebookRight /></button
			>
		</form>
	</td>
</tr>
