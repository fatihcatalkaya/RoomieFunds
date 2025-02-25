<script lang="ts">
	import { page } from "$app/state";
	import type { Account, TransactionSaldoDto } from "$lib/client";
    import { deleteApiTransactionByTransactionId, getApiAccountByAccountId, getApiTransactionAccountByAccountId } from "$lib/client";
	import TransactionInsert from "$lib/components/TransactionInsert.svelte";
	import TransactionDisplayRow from "$lib/components/TransactionDisplayRow.svelte";
    import MdiPencilPlus from '~icons/mdi/pencil';
	import MdiDelete from '~icons/mdi/delete';
    import MdiScriptText from '~icons/mdi/script-text';

    let lastUpdated = $state(Date.now());

    let fetchQuery: Promise<{transactions: TransactionSaldoDto[], account: Account}> = $derived.by(async () => {
        let _ = lastUpdated;

        let transactionQuery = getApiTransactionAccountByAccountId({
            path: {
                accountId: Number(page.params.accountId), // if this throws we catch this in the ui
            }
        });

        let accountQuery = getApiAccountByAccountId({
            path: {
                accountId: Number(page.params.accountId), // if this throws we catch this in the ui
            }
        });

        let [ transactions, account ] = await Promise.all([transactionQuery, accountQuery])

        if (transactions.error) {
            throw transactions.error
        } else if (account.error) {
            throw account.error
        } else {
            return {
                transactions: transactions.data!,
                account: account.data!,
            }
        }
    });

    let transactionDeleteModal: HTMLDialogElement;
    let transactionDeleteData: TransactionSaldoDto = $state({});

    function deleteTransaction(dto: TransactionSaldoDto) {
        transactionDeleteData = dto;
        transactionDeleteModal.showModal();
    }

    async function reallyDeleteTransaction(dto: TransactionSaldoDto) {
        let query = await deleteApiTransactionByTransactionId({
            path: {
                transactionId: dto.transaction?.id!
            }
        });

        if (query.error) {
            console.error(query.error)
        } else {
            lastUpdated = Date.now()
        }
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


{#await fetchQuery}

    <span>Loading data...</span>

{:then {transactions, account}}

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

    <div class="rounded-box border-base-content/5 overflow-x-auto overflow-y-scroll border border-slate-300 text-nowrap">
        <table class="table-zebra table-pin-rows table">
            <thead class="text-neutral">
                <tr>
                    <th>Buchungsdatum</th>
                    <th>Beschreibung</th>
                    <th>Buchen Gegen</th>
                    <th class="text-right">Betrag</th>
                    <th class="text-right">Saldo</th>
                    <th>Bearbeiten</th>
                </tr>
            </thead>
            <tbody>
                {#each transactions! as dto}
                    <TransactionDisplayRow {dto} {account} refreshTransaction={() => lastUpdated = Date.now()} tryDelete={() => deleteTransaction(dto)} />
                {/each}
                
                <TransactionInsert parentAccountId={account.id!} refreshTransactions={() => lastUpdated = Date.now()}/>
            </tbody>
        </table>
    </div>
{:catch error}
    Unable to fetch transaction!

    <code>
        {JSON.stringify(error)}
    </code>
{/await}
