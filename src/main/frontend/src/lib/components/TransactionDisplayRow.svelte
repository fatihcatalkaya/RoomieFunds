<script lang="ts">
	import {
		deleteApiTransactionByTransactionIdReceipt,
		getApiAccount,
		getApiTransactionByTransactionIdReceipt,
		patchApiTransactionByTransactionId,
		postApiTransactionByTransactionIdReceipt,
		type Account,
		type TransactionSaldoDto
	} from '$lib/client';
	import { formatEuroCents, formatIsoDate } from '$lib/formatter';
	import RightArrowMarker from './RightArrowMarker.svelte';
	import MdiPencil from '~icons/mdi/pencil';
	import MdiDelete from '~icons/mdi/delete';
	import MdiCancel from '~icons/mdi/cancel';
	import MdiCheck from '~icons/mdi/check-bold';
	import MdiClose from '~icons/mdi/close-bold';
	import MdiDownload from '~icons/mdi/download';
	import EuroInput from '$lib/components/EuroInput.svelte';
	import { bookingFor, signedCentsFor } from '$lib/transactionAmount';
	import ReceiptFileInput from '$lib/components/ReceiptFileInput.svelte';

	let {
		dto,
		account,
		refreshTransaction,
		tryDelete,
		tryDeleteReceipt
	}: {
		dto: TransactionSaldoDto;
		account: Account;
		refreshTransaction: () => void;
		tryDelete: () => void;
		tryDeleteReceipt: () => void;
	} = $props();

	let editToggle = $state(false);

	let date: string = $state(dto.transaction?.valueDate!);
	let description: string = $state(dto.transaction?.description!);
	let receiptFile: FileList | undefined = $state();

	// The signed effect on the opened account, in cents — the same number the
	// Betrag column shows, and the same number the user edits.
	// svelte-ignore state_referenced_locally
	let amount: number | null = $state(signedCentsFor(dto.transaction!, account.id!));

	// a $derived(...) would make sense here but we can't bind to that. Value is manually set in allowEdit()
	// svelte-ignore state_referenced_locally
	let bookAccountId: number | undefined = $state(
		dto.transaction?.sourceAccountId === account.id
			? dto.transaction?.targetAccountId
			: dto.transaction?.sourceAccountId
	);

	let accountList = $derived.by(async () => {
		if (editToggle) {
			let query = await getApiAccount();
			if (query.error) {
				throw query.error;
			} else {
				return query.data!;
			}
		} else {
			return [];
		}
	});

	async function allowEdit() {
		date = dto.transaction?.valueDate!;
		description = dto.transaction?.description!;
		amount = signedCentsFor(dto.transaction!, account.id!);
		bookAccountId =
			dto.transaction?.sourceAccountId === account.id
				? dto.transaction?.targetAccountId
				: dto.transaction?.sourceAccountId;

		editToggle = true;
	}

	async function submitChange(event: SubmitEvent) {
		event.preventDefault();

		const counterAccount = (await accountList).find((entry) => entry.id === bookAccountId);
		if (!counterAccount || amount == null || amount === 0) {
			return;
		}

		const query = await patchApiTransactionByTransactionId({
			path: {
				transactionId: dto.transaction?.id!
			},
			body: {
				valueDate: new Date(date!).toISOString().substring(0, 10),
				description: description!,
				...bookingFor(amount, account, counterAccount)
			}
		});

		if (receiptFile && receiptFile.length > 0) {
			await postApiTransactionByTransactionIdReceipt({
				path: {
					transactionId: dto.transaction?.id!
				},
				body: {
					receipt: receiptFile[0]
				}
			});
		}

		if (query.error) {
			console.error(query.error);
		} else {
			refreshTransaction();
		}
	}

	let receiptDownloadIsLoading = $state(false);

	async function showReceipt() {
		// Open the tab synchronously, still inside the click handler's user activation,
		// so iOS Safari does not block it once the fetch below resolves.
		const newTab = window.open('', '_blank');
		receiptDownloadIsLoading = true;
		try {
			let query = await getApiTransactionByTransactionIdReceipt({
				path: { transactionId: dto.transaction?.id! }
			});
			const blob = query.data as Blob | undefined;
			if (!blob) {
				newTab?.close();
				return;
			}
			openPDFInNewTab(blob, newTab);
		} catch (e) {
			// Don't leave the pre-opened blank tab behind if the download failed.
			newTab?.close();
			console.error(e);
		} finally {
			receiptDownloadIsLoading = false;
		}
	}

	// Assuming arrayBuffer is already available
	function openPDFInNewTab(blob: Blob, newTab: Window | null) {
		const url = URL.createObjectURL(blob);
		if (!newTab) {
			// Popup was blocked despite the synchronous window.open attempt; fall back
			// to the previous behaviour of opening a new tab after the blob resolves.
			newTab = window.open(url, '_blank');
		} else {
			newTab.location.href = url;
		}
		if (newTab) {
			newTab.onbeforeunload = () => {
				URL.revokeObjectURL(url);
			};
		}
	}
</script>

{#if !editToggle}
	{@const signedAmount = signedCentsFor(dto.transaction!, account.id!)}
	<tr>
		<td>{formatIsoDate(dto.transaction?.valueDate)}</td>
		<td>{dto.transaction?.description}</td>
		<td class="text-nowrap">
			{#if dto.transaction?.sourceAccountName === account.name}
				{#each dto.targetAccountNameParts! as part, i}
					{part}
					{#if i < dto.targetAccountNameParts!.length - 1}
						<RightArrowMarker />
					{/if}
				{/each}
			{:else}
				{#each dto.sourceAccountNameParts! as part, i}
					{part}
					{#if i < dto.sourceAccountNameParts!.length - 1}
						<RightArrowMarker />
					{/if}
				{/each}
			{/if}
		</td>
		<td class="text-center">
			{#if dto.transaction!.hasReceipt}
				<button class="btn btn-primary h-8 w-8 p-0 text-lg" onclick={showReceipt}>
					{#if receiptDownloadIsLoading}
						<MdiDownload class="text-primary-content/30" />
						<span class="loading loading-spinner loading-sm absolute inset-auto z-10 mx-auto"
						></span>
					{:else}
						<MdiDownload />
					{/if}
				</button>
				<button class="btn btn-error h-8 w-8 p-0 text-lg" onclick={tryDeleteReceipt}>
					<MdiDelete />
				</button>
			{:else}
				<MdiClose class="text-error mx-auto" />
			{/if}
		</td>
		<td class="text-right {signedAmount < 0 ? 'font-semibold text-red-500' : ''}">
			{formatEuroCents(signedAmount)}
		</td>
		{#if dto.saldo! >= 0}
			<td class="text-right">{formatEuroCents(dto.saldo!)}</td>
		{:else}
			<td class="text-right font-semibold text-red-500">{formatEuroCents(dto.saldo!)}</td>
		{/if}
		<td>
			<button
				title="Buchung {dto.transaction?.id} Bearbeiten"
				onclick={allowEdit}
				class="btn btn-primary m-0 h-8 w-8 p-0 text-lg"><MdiPencil /></button
			>
			<button
				title="Buchung {dto.transaction?.id} Löschen!"
				onclick={tryDelete}
				class="btn hover:btn-error focus:btn-error m-0 h-8 w-8 p-0 text-lg"><MdiDelete /></button
			>
		</td>
	</tr>
{:else}
	<tr>
		<td>
			<input
				form="transaction-new-form-{dto.transaction?.id}"
				bind:value={date}
				type="date"
				class="input"
				required
			/>
		</td>
		<td>
			<input
				form="transaction-new-form-{dto.transaction?.id}"
				bind:value={description}
				type="text"
				class="input"
				placeholder="Beschreibung"
			/>
		</td>
		<td>
			<select
				form="transaction-new-form-{dto.transaction?.id}"
				bind:value={bookAccountId}
				class="select"
			>
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
			<ReceiptFileInput bind:files={receiptFile} />
		</td>
		<td>
			<EuroInput
				class="input min-w-20"
				form="transaction-new-form-{dto.transaction?.id}"
				bind:value={amount}
				allowNegative
			/>
		</td>
		<td></td>
		<td>
			<form
				class="m-0 inline p-0"
				id="transaction-new-form-{dto.transaction?.id}-{dto.transaction?.id}"
				onsubmit={submitChange}
			>
				<button title="Speichern" class="btn btn-success m-0 h-8 w-8 p-0 text-lg"
					><MdiCheck /></button
				>
			</form>
			<button
				title="Abbrechen"
				onclick={() => (editToggle = !editToggle)}
				class="btn btn-error btn-error m-0 h-8 w-8 p-0 text-lg"><MdiCancel /></button
			>
		</td>
	</tr>
{/if}
