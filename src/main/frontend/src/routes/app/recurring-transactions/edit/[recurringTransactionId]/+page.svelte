<script lang="ts">
	import { goto } from "$app/navigation";
	import { page } from "$app/state";
	import { deleteApiRecurringTransactionByRecurringTransactionId, getApiAccount, patchApiRecurringTransactionByRecurringTransactionId, type GetRecurringTransactionDto, type UpdateRecurringTransactionDto } from "$lib/client";
    import MdiDelete from "~icons/mdi/delete";

    let { data } = $props();

    let recurringTransaction = data.data as GetRecurringTransactionDto;
    let error = data.error;

    let accountsQuery = $derived.by(async () => {
        const accountsQuery = await getApiAccount();

        if (accountsQuery.error) {
            throw accountsQuery.error;
        } else {
            return accountsQuery.data;
        }
    });

    let formData: UpdateRecurringTransactionDto = $state({
        name: recurringTransaction.name,
        sourceAccountId: recurringTransaction.sourceAccountId,
        targetAccountId: recurringTransaction.targetAccountId,
        amount: recurringTransaction.amount,
        valueDayOfMonth: recurringTransaction.valueDayOfMonth,
        transactionDescription: recurringTransaction.transactionDescription,
    });

    async function updateRecurringTransaction() {
        let query = await patchApiRecurringTransactionByRecurringTransactionId({
            path: {
                recurringTransactionId: recurringTransaction.id!
            },
            body: {...formData},
        });

        if (query.error) {
            console.error(query.error)
        } else {
            goto("../")
        }

        return false
    }
    
    let modalElement: HTMLDialogElement;

    async function deleteRecurringTransaction() {
        modalElement!.showModal();
    }

    async function reallyDeleteRecurringTransaction() {
        let query = await deleteApiRecurringTransactionByRecurringTransactionId({
            path: {
                recurringTransactionId: recurringTransaction.id!,
            }
        });

        if (query.error) {
            console.error(query.error)
        } else {
            goto("../")
        }

        return true
    }
</script>

<dialog class="modal" bind:this={modalElement}>
    <div class="modal-box">
      <h3 class="text-lg font-bold">{name} löschen</h3>
      <p class="py-4">Bist du dir sicher, dass du Dauerauftrag {recurringTransaction.id} löschen willst?</p>
      <div class="modal-action">
        <form method="dialog" class="join">
            <button class="btn btn-error join-item" onclick={reallyDeleteRecurringTransaction}>Löschen</button>
            <button class="btn join-item">Abbrechen</button>
        </form>
      </div>
    </div>
    <form method="dialog" class="modal-backdrop">
      <button>close</button>
    </form>
  </dialog>

{#if !error}
    <div class="inline-flex items-center w-full my-4">
        <h1 class="flex-grow text-2xl font-bold">
            Produkt {page.params.id} Bearbeiten
        </h1>
        <button class="btn btn-error h-8 w-8 p-0 m-0 text-lg" onclick={deleteRecurringTransaction}>
            <MdiDelete/>
        </button>
    </div>

    <form method="dialog" class="grid grid-cols-1 gap-2">
        <label class="w-full flex items-center">
            <span class="w-1/4">ID</span>
            <input type="text" class="input w-3/4" value="{recurringTransaction.id}" disabled />
        </label>
        <label class="w-full flex items-center">
            <span class="w-1/4">Name</span>
            <input type="text" class="input w-3/4" bind:value={formData.name} />
        </label>
        <label class="w-full flex items-center">
            <span class="w-1/4">Buchen Von</span>
            <select class="select w-3/4" bind:value={formData.sourceAccountId}>
                {#await accountsQuery}
                    <option value="" disabled>Loading accounts...</option>
                {:then accounts}
                    {#each accounts! as account}
                        <option value="{account.id}">{account.name}</option>
                    {/each}
                {:catch}
                    <option value="" disabled>Error while fetching accounts</option>
                {/await}
            </select>
        </label>
        <label class="w-full flex items-center">
            <span class="w-1/4">Buchen Nach</span>
            <select class="select w-3/4" bind:value={formData.targetAccountId}>
                {#await accountsQuery}
                    <option value="" disabled>Loading accounts...</option>
                {:then accounts}
                    {#each accounts! as account}
                        <option value="{account.id}">{account.name}</option>
                    {/each}
                {:catch}
                    <option value="" disabled>Error while fetching accounts</option>
                {/await}
            </select>
        </label>
        <label class="w-full flex items-center">
            <span class="w-1/4">Betrag</span>
            <input type="number" class="input w-3/4" bind:value={formData.amount} />
        </label>
        <label class="w-full flex items-center">
            <span class="w-1/4"><span class="after:content-['Tag'] md:after:content-['Wertstellungstag']"></span> des Monats</span>
            <input type="number" class="input w-3/4" bind:value={formData.valueDayOfMonth} />
        </label>
        <label class="w-full flex items-center">
            <span class="w-1/4 after:content-['Beschr.'] md:after:content-['Beschreibung'] lg:after:content-['Transaktionsbeschreibung']"></span>
            <input type="text" class="input w-3/4" bind:value={formData.transactionDescription} />
        </label>
        <div class="join w-full mt-2">
            <a href="/app/recurring-transactions" class="join-item btn btn-warn w-1/2">
                Zurück
            </a>
            <input class="join-item btn btn-success w-1/2" value="Speichern" type="submit" onclick={updateRecurringTransaction}>
        </div>
    </form>
{:else}
    Error while fetching product!
    <pre class="mt-4">{JSON.stringify(error, null, 2)}</pre>
{/if}
