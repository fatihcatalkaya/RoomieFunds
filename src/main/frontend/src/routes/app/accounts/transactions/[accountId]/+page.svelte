<script lang="ts">
	import type { Account, TransactionSaldoDto } from "$lib/client";
    import { getApiAccountByAccountId, getApiTransactionAccountByAccountId } from "$lib/client";

    import Dingsbums from "$lib/components/dingsbums.svelte";
	import { formatEuroCents } from "$lib/formatter";
	import MdiPencilPlus from '~icons/mdi/pencil';
	import MdiDelete from '~icons/mdi/delete';
	import { page } from "$app/state";

    // fetch transactions in derived field because the query requires more complex querying
    let fetchQuery: Promise<{transactions: TransactionSaldoDto[], account: Account}> = $derived.by(async () => {
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
</script>

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
        <a href="/app/persons/create" class="btn btn-warning h-8 w-8 p-0 m-0 text-lg">
            <MdiPencilPlus/>
        </a>
        <button href="/app/persons/create" class="btn btn-error h-8 w-8 p-0 m-0 text-lg" disabled>
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
                </tr>
            </thead>
            <tbody>
                {#each transactions! as dto}
                    <tr>
                        <td>{dto.transaction?.valueDate}</td>
                        <td>{dto.transaction?.description}</td>
                        <td>
                            {#if dto.transaction?.sourceAccountName === account.name}
                                {#each dto.targetAccountNameParts! as part, i}
                                    {part}
                                    {#if i < dto.targetAccountNameParts!.length - 1}
                                        <Dingsbums/>  
                                    {/if}
                                {/each}
                            {:else}
                                {#each dto.sourceAccountNameParts! as part, i}
                                    {part}
                                    {#if i < dto.sourceAccountNameParts!.length - 1}
                                        <Dingsbums/>  
                                    {/if}
                                {/each}
                            {/if}
                        </td>
                        {#if dto.transaction!.sourceAccountName === account.name}
                        <td class="text-right font-semibold text-red-500">{formatEuroCents(dto.transaction?.amount! * -1)}</td>     
                        {:else}
                            <td class="text-right">{formatEuroCents(dto.transaction?.amount!)}</td>
                        {/if}
                        {#if dto.saldo! >= 0}
                            <td class="text-right">{formatEuroCents(dto.saldo!)}</td>
                        {:else}
                            <td class="text-right font-semibold text-red-500">{formatEuroCents(dto.saldo!)}</td>                    
                        {/if}
                    </tr>
                {/each}
            </tbody>
        </table>
    </div>
{:catch error}
    Unable to fetch transaction!

    <code>
        {JSON.stringify(error)}
    </code>
{/await}
