<script lang="ts">
	import { page } from "$app/state";
	import type { TransactionSaldoDto } from "$lib/client";
    import { deleteApiTransactionByTransactionId, deleteApiTransactionByTransactionIdReceipt, getApiTransactionAccountByAccountId } from "$lib/client";
	import TransactionInsert from "$lib/components/TransactionInsert.svelte";
	import TransactionDisplayRow from "$lib/components/TransactionDisplayRow.svelte";
    import MdiPencilPlus from '~icons/mdi/pencil';
	import MdiDelete from '~icons/mdi/delete';
    import MdiScriptText from '~icons/mdi/script-text';
	import type { PageProps } from "./$types";
	import ErrorAlert from "$lib/components/ErrorAlert.svelte";
	import { createFetchQuery } from "./transactionsLoadQuery";

    const { data }: PageProps = $props();
    const { fetchQuery } = data;

    let currentQuery = $state.raw(fetchQuery);
    let updating = $state(false);

    let transactionDeleteModal: HTMLDialogElement;
    let transactionDeleteData: TransactionSaldoDto = $state({});

    function deleteTransaction(dto: TransactionSaldoDto) {
        transactionDeleteData = dto;
        transactionDeleteModal.showModal();
    }

    function refreshTransactions() {
        createFetchQuery(Number(page.params.accountId)).then(loadedData => {
            currentQuery = new Promise((resolve) => resolve(loadedData))
            updating = false;
        });
    }

    async function reallyDeleteTransaction(dto: TransactionSaldoDto) {
        updating = true;

        let query = await deleteApiTransactionByTransactionId({
            path: {
                transactionId: dto.transaction?.id!
            }
        });

        if (query.error) {
            console.error(query.error)
        }

        refreshTransactions();
    }

    async function deleteReceipt(transactionId: number) {
        const query = await deleteApiTransactionByTransactionIdReceipt({ path: { transactionId: transactionId } });
        refreshTransactions();
    }
</script>

<dialog class="modal" bind:this={transactionDeleteModal}>
	<div class="modal-box">
		<h3 class="text-lg font-bold">{transactionDeleteData.transaction?.description} löschen</h3>
		<p class="py-4">Bist du dir sicher, dass du Transaktion {transactionDeleteData.transaction?.id} löschen willst?</p>
		<div class="modal-action">
			<form method="dialog" class="join">
				<button class="btn btn-error join-item" onclick={() => reallyDeleteTransaction(transactionDeleteData)}>Löschen</button>
				<button class="btn join-item">Abbrechen</button>
			</form>
		</div>
	</div>
	<form method="dialog" class="modal-backdrop">
		<button>close</button>
	</form>
</dialog>

{#await currentQuery}
    <div class="flex mt-4">
        <span class="loading loading-spinner loading-lg mx-auto"></span>
    </div>
{:then [transactions, account]}
    <div class="inline-flex items-center w-full my-4 gap-2">
        <h1 class="text-2xl font-bold pr-2">
            {#each account.name!.split(':') as part, i}
                {part}
                {#if i < account.name!.split(':').length - 1}
                    <li class="inline-flex"><svg class="h-6 w-6 text-gray-400" fill="currentColor" viewBox="2 -2 18 18" xmlns="http://www.w3.org/2000/svg" ><path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"></path></svg></li>
                {/if}
            {/each}
        </h1>
        {#if account.active}
            <div class="badge badge-lg badge-primary font-bold md:block hidden">Aktiv</div>
        {:else}
            <div class="badge badge-lg badge-primary font-bold md:block hidden">Passiv</div>
        {/if}
        <div class="flex-grow"></div>
        <a href="/app/accounts/log/{account.id}" title="Änderungsprotokoll" class="btn btn-primary h-8 w-8 p-0 m-0 text-lg">
            <MdiScriptText/>
        </a>
        <a href="/app/persons/create" class="btn btn-warning h-8 w-8 p-0 m-0 text-lg">
            <MdiPencilPlus/>
        </a>
        <button class="btn btn-error h-8 w-8 p-0 m-0 text-lg" disabled>
            <MdiDelete/>
        </button>
    </div>

    <div class="rounded-box border-base-content/5 overflow-x-auto overflow-y-scroll border border-slate-300 {updating ? "pointer-events-none opacity-40" : ""}">
        <table class="table-zebra table-pin-rows table">
            <thead class="text-neutral">
                <tr>
                    <th>Buchungsdatum</th>
                    <th>Beschreibung</th>
                    <th>Buchen Gegen</th>
                    <th class="text-center">Beleg</th>
                    <th class="text-right">Betrag</th>
                    <th class="text-right">Saldo</th>
                    <th>Bearbeiten</th>
                </tr>
            </thead>
            <tbody>
                {#key transactions}
                    {#if transactions.length == 0}
                        <tr>
                            <td colspan="6" class="text-lg p-6 pl-4">Auf diesem Konto sind keine Buchungen verzeichnet.</td>
                        </tr>    
                    {:else}
                        {#each transactions! as dto}
                            <TransactionDisplayRow {dto} {account} refreshTransaction={refreshTransactions} tryDelete={() => deleteTransaction(dto)} tryDeleteReceipt={() => deleteReceipt(dto.transaction?.id!)}/>
                        {/each}
                    {/if}
                
                    <TransactionInsert parentAccountId={account.id!} refreshTransactions={refreshTransactions}/>
                {/key}
            </tbody>
        </table>
    </div>
{:catch error}
    <ErrorAlert>
        Konnte die Transaktionen für Konto {page.params.accountId} nicht laden!
    </ErrorAlert>
    <code class="w-full mt-4">
        {JSON.stringify(error, null, 2)}
    </code>
{/await}
