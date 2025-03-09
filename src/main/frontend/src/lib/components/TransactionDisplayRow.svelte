<script lang="ts">
	import { deleteApiTransactionByTransactionIdReceipt, getApiAccount, getApiTransactionByTransactionIdReceipt, patchApiTransactionByTransactionId, postApiTransactionByTransactionIdReceipt, type Account, type TransactionSaldoDto } from "$lib/client";
	import { formatEuroCents } from "$lib/formatter";
	import RightArrowMarker from "./RightArrowMarker.svelte";
    import MdiPencil from "~icons/mdi/pencil";
    import MdiDelete from "~icons/mdi/delete";
    import MdiCancel from "~icons/mdi/cancel";
	import MdiCheck from '~icons/mdi/check-bold';
	import MdiClose from '~icons/mdi/close-bold';
	import MdiUpload from '~icons/mdi/upload';
	import MdiDownload from '~icons/mdi/download';

    let {
        dto,
        account,
        refreshTransaction,
        tryDelete,
        tryDeleteReceipt,
    }: {
        dto: TransactionSaldoDto,
        account: Account,
        refreshTransaction: () => void,
        tryDelete: () => void,
        tryDeleteReceipt: () => void,
    } = $props();

    type BookDirection = "decrease" | "increase";

    let editToggle = $state(false);
    
    let direction: BookDirection = $state(dto.transaction?.sourceAccountId! === account.id ? "decrease" : "increase");
    let date: string = $state(dto.transaction?.valueDate!);
    let description: string = $state(dto.transaction?.description!);
    let floatAmount: number = $state(dto.transaction?.amount! / 100.0);
    let receiptFile: FileList | undefined = $state();

    // a $derived(...) would make sense here but we can't bind to that. Value is manually set in allowEdit()
    // svelte-ignore state_referenced_locally
    let bookAccountId: number | undefined = $state(direction === "decrease" ? dto.transaction?.targetAccountId! : dto.transaction?.sourceAccountId!);

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
    })

    let doChangeAmountSign = $derived.by(() => {
        console.log(dto.transaction?.sourceAccountActive, dto.transaction?.targetAccountActive)
        if (dto.transaction?.sourceAccountActive! !== dto.transaction?.targetAccountActive!) {
            return false;
        } else {
            return dto.transaction?.sourceAccountId === account.id
        }
    });

    async function allowEdit() {
        direction = dto.transaction?.sourceAccountId! === account.id ? "decrease" : "increase";
        date = dto.transaction?.valueDate!;
        description = dto.transaction?.description!;
        bookAccountId = direction === "decrease" ? dto.transaction?.targetAccountId! : dto.transaction?.sourceAccountId!;
        floatAmount = dto.transaction?.amount! / 100.0;

        editToggle = true;
    }

    async function submitChange(event: SubmitEvent) {
        event.preventDefault();
        
        const query = await patchApiTransactionByTransactionId({
            path: {
                transactionId: dto.transaction?.id!
            },
            body: {
                valueDate: new Date(date!).toISOString().substring(0, 10),
                description: description!,
                amount: Math.round(floatAmount! * 100.0),
                sourceAccountId: direction === "decrease" ? account.id! : bookAccountId!,
                targetAccountId: direction === "decrease" ? bookAccountId! : account.id!,
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
            })
        }

        if (query.error) {
            console.error(query.error);
        } else {
            refreshTransaction();
        }
    }

    let receiptDownloadIsLoading = $state(false);

    async function showReceipt() {
        receiptDownloadIsLoading = true;
		let query = await getApiTransactionByTransactionIdReceipt({ path: { transactionId: dto.transaction?.id! } });
		openPDFInNewTab(query.data as Blob)
        receiptDownloadIsLoading = false;
	}

	// Assuming arrayBuffer is already available
	function openPDFInNewTab(blob: Blob) {
		const url = URL.createObjectURL(blob);
		const newTab = window.open(url, '_blank')!;
		newTab.onbeforeunload = () => {
			URL.revokeObjectURL(url);
		};
	}
</script>

{#if !editToggle}
    <tr>
        <td>{dto.transaction?.valueDate}</td>
        <td>{dto.transaction?.description}</td>
        <td class="text-nowrap">
            {#if dto.transaction?.sourceAccountName === account.name}
                {#each dto.targetAccountNameParts! as part, i}
                    {part}
                    {#if i < dto.targetAccountNameParts!.length - 1}
                        <RightArrowMarker/>  
                    {/if}
                {/each}
            {:else}
                {#each dto.sourceAccountNameParts! as part, i}
                    {part}
                    {#if i < dto.sourceAccountNameParts!.length - 1}
                        <RightArrowMarker/>  
                    {/if}
                {/each}
            {/if}
        </td>
        <td class="text-center">
            {#if dto.transaction!.hasReceipt}
                <button class="btn btn-primary text-lg w-8 h-8 p-0" onclick={showReceipt}>
                    {#if receiptDownloadIsLoading}
                        <MdiDownload class="text-primary-content/30" />
                        <span class="absolute inset-auto z-10 loading loading-spinner loading-sm mx-auto"></span>
                    {:else}
                        <MdiDownload/>
                    {/if}
                </button>
                <button class="btn btn-error text-lg w-8 h-8 p-0" onclick={tryDeleteReceipt}>
                    <MdiDelete/>
                </button>
            {:else}
                <MdiClose class="text-error mx-auto" />
            {/if}
        </td>
        {#if doChangeAmountSign && dto.transaction?.amount! > 0}
            <td class="text-right font-semibold text-red-500">{formatEuroCents(dto.transaction?.amount! * -1)}</td>     
        {:else if doChangeAmountSign && dto.transaction?.amount! < 0}
            <td class="text-right">{formatEuroCents(dto.transaction?.amount! * -1)}</td>     
        {:else if dto.transaction?.amount! < 0}
            <td class="text-right font-semibold text-red-500">{formatEuroCents(dto.transaction?.amount!)}</td>
        {:else}
            <td class="text-right">{formatEuroCents(dto.transaction?.amount!)}</td>
        {/if}
        {#if dto.saldo! >= 0}
            <td class="text-right">{formatEuroCents(dto.saldo!)}</td>
        {:else}
            <td class="text-right font-semibold text-red-500">{formatEuroCents(dto.saldo!)}</td>                    
        {/if}
        <td>
            <button title="Buchung {dto.transaction?.id} Bearbeiten" onclick={allowEdit} class="btn btn-primary h-8 w-8 p-0 m-0 text-lg"><MdiPencil/></button>
            <button title="Buchung {dto.transaction?.id} Löschen!" onclick={tryDelete} class="btn hover:btn-error focus:btn-error h-8 w-8 p-0 m-0 text-lg"><MdiDelete/></button>
        </td>
    </tr>
{:else}
    <tr>
        <td>
            <input form="transaction-new-form-{dto.transaction?.id}" bind:value={date} type="date" class="input">
        </td>
        <td>
            <input form="transaction-new-form-{dto.transaction?.id}" bind:value={description} type="text" class="input" placeholder="Beschreibung">
        </td>
        <td>
            <select form="transaction-new-form-{dto.transaction?.id}" bind:value={bookAccountId} class="select">
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
            <label class="btn btn-primary text-lg h-8 w-8 p-0">
                <MdiUpload/>
                <input type="file" class="hidden" bind:files={receiptFile} capture="environment" multiple={false} accept="image/*,pdf" />
            </label>
            <button class="btn btn-error text-lg w-8 h-8 p-0" disabled={!(receiptFile && receiptFile.length > 0)} onclick={() => (receiptFile = undefined)}>
                <MdiClose/>
            </button>
        </td>
        <td>
            <label class="input" lang="de">
                <input lang="de" form="transaction-new-form-{dto.transaction?.id}" bind:value={floatAmount} type="number" step="0.01">
                €
            </label>
        </td>
        <td>
            <label>
                <input form="transaction-new-form-{dto.transaction?.id}" type="radio" class="radio" bind:group={direction} name="book-dir" value="decrease" defaultChecked disabled={dto.transaction?.sourceAccountActive != dto.transaction?.targetAccountActive}>
                Abnahme
            </label>
            <br/>
            <label>
                <input form="transaction-new-form-{dto.transaction?.id}" type="radio" class="radio" bind:group={direction} name="book-dir" value="increase" disabled={dto.transaction?.sourceAccountActive != dto.transaction?.targetAccountActive}>
                Zunahme
            </label>
        </td>
        <td>
            <form class="p-0 m-0 inline" id="transaction-new-form-{dto.transaction?.id}-{dto.transaction?.id}" onsubmit={submitChange}>
                <button title="Speichern" class="btn btn-success h-8 w-8 p-0 m-0 text-lg"><MdiCheck/></button>
            </form>
            <button title="Abbrechen" onclick={() => editToggle = !editToggle} class="btn btn-error btn-error h-8 w-8 p-0 m-0 text-lg"><MdiCancel/></button>
        </td>
    </tr>
{/if}
