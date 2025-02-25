<script lang="ts">
	import { getApiAccount, postApiTransaction } from '$lib/client';
	import { error } from '@sveltejs/kit';
    import MdiChequebookRight from '~icons/mdi/chequebook-arrow-left';

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
        }

        refreshTransactions()
    }
</script>

<tr>
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
                    <option value={accountEntry.id}>{accountEntry.name}</option>
                {/each}
            {:catch error}
                <option value="" disabled>Error fetching accounts!</option>
            {/await}
        </select>
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