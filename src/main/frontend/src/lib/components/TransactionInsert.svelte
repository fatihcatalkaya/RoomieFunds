<script lang="ts">
	import { getApiAccount, getApiTransactionByTransactionIdReceipt, postApiTransaction, postApiTransactionByTransactionIdReceipt } from '$lib/client';
	import { error } from '@sveltejs/kit';
    import MdiChequebookRight from '~icons/mdi/chequebook-arrow-left';
    import MdiUpload from '~icons/mdi/upload';
    import MdiClose from '~icons/mdi/close-bold';

    const accountList = $derived.by(async () => {
        const accountQuery = await getApiAccount();

        if (accountQuery.error) {
            throw error;
        } else {
            return accountQuery.data!;
        }
    });

    type BookDirection = "decrease" | "increase";
    type TransactionInsertProps = {
        parentAccountId: number;
        refreshTransactions: () => void;
    }

    let { parentAccountId, refreshTransactions }: TransactionInsertProps = $props();

    let date: string = $state(new Date(Date.now()).toDateString());
    let description: string = $state("");
    let bookAccountId: number | undefined = $state();
    let floatAmount: number = $state(0.0);
    let direction: BookDirection = $state("decrease");
    let files: FileList | undefined = $state();

    async function submitTransaction(event: SubmitEvent) {
        event.preventDefault();

        const query = await postApiTransaction({
            body: {
                valueDate: new Date(date).toISOString().substring(0, 10),
                description,
                sourceAccountId: direction == "decrease" ? parentAccountId : bookAccountId,
                targetAccountId: direction == "decrease" ? bookAccountId : parentAccountId,
                amount: floatAmount * 100,
            }
        });

        if (query.error) {
            console.error(error);
        } else if (files && files.length > 0) {
            console.log(files);
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

        refreshTransactions()
    }
</script>

<tr class="bg-base-200">
    <td>
        <input form="transaction-new-form" bind:value={date} type="date" class="input">
    </td>
    <td>
        <input form="transaction-new-form" bind:value={description} type="text" class="input" placeholder="Beschreibung">
    </td>
    <td>
        <select form="transaction-new-form" bind:value={bookAccountId} class="select">
            {#await accountList}
                <option value="" disabled>Loading...</option>
            {:then accountList}
                {#each accountList as accountEntry}
                    {#if accountEntry.id !== parentAccountId}
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
            <input type="file" class="hidden" bind:files={files} capture="environment" multiple={false} accept="image/*,pdf" />
        </label>
        <button class="btn btn-error text-lg w-8 h-8 p-0" disabled={!(files && files.length > 0)} onclick={() => (files = undefined)}>
            <MdiClose/>
        </button>
    </td>
    <td>
        <label class="input" lang="de">
            <input dir="rtl" lang="de" form="transaction-new-form" bind:value={floatAmount} type="number" min="0.00" step="0.01">
            €
        </label>
    </td>
    <td>
        <label>
            <input form="transaction-new-form" type="radio" class="radio" bind:group={direction} name="book-dir" value="decrease" defaultChecked>
            Abnahme
        </label>
        <br/>
        <label>
            <input form="transaction-new-form" type="radio" class="radio" bind:group={direction} name="book-dir" value="increase">
            Zunahme
        </label>
    </td>
    <td>
        <form id="transaction-new-form" onsubmit={submitTransaction}>
            <button title="Buchen" class="btn btn-success h-8 w-17 p-0 m-0 text-lg"><MdiChequebookRight/></button>
        </form>
    </td>
</tr>