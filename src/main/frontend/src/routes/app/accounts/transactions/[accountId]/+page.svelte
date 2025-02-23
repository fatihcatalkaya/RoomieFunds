<script lang="ts">
	import type { Account } from "$lib/client";
    import Dingsbums from "$lib/components/dingsbums.svelte";
	import { formatEuroCents } from "$lib/formatter";
	import MdiPencilPlus from '~icons/mdi/pencil';
	import MdiDelete from '~icons/mdi/delete';

    type Transaction = {
        valueDate: string;
        description: string;
        source: string;
        target: string;
        amount: number;
    }

    type DisplayTransaction = {
        valueDate: string;
        description: string;
        source: string;
        sourceParts: string[];
        target: string;
        targetParts: string[];
        amount: number;
        saldo: number;
        sign: boolean;
    }

    let data: Array<Transaction> = [
        {
            valueDate: "01.01.2025",
            description: "Flurbeitrag 01/25",
            source: "Passiv:Bewohner:Yusuf R403",
            target: "Passiv:Flurkasse",
            amount: 800,
        },
        {
            valueDate: "01.01.2025",
            description: "Flurbeitrag 01/25",
            source: "Passiv:Bewohner:Fatih R405",
            target: "Passiv:Flurkasse",
            amount: 800,
        },
        {
            valueDate: "01.01.2025",
            description: "Flurbeitrag 01/25",
            source: "Passiv:Bewohner:Christian R406",
            target: "Passiv:Flurkasse",
            amount: 800,
        },
        {
            valueDate: "17.01.2025",
            description: "Einkauf LIDL",
            source: "Passiv:Flurkasse",
            target: "Passiv:Christian R406",
            amount: 1199,
        },
        {
            valueDate: "24.01.2025",
            description: "GEZ Q4 2024",
            source: "Passiv:Flurkasse",
            target: "Passiv:Yusuf R403",
            amount: 400,
        },
        {
            valueDate: "01.02.2025",
            description: "Flurbeitrag 02/25",
            source: "Passiv:Bewohner:Yusuf R403",
            target: "Passiv:Flurkasse",
            amount: 800,
        },
        {
            valueDate: "01.02.2025",
            description: "Flurbeitrag 02/25",
            source: "Passiv:Bewohner:Fatih R405",
            target: "Passiv:Flurkasse",
            amount: 800,
        },
        {
            valueDate: "01.02.2025",
            description: "Flurbeitrag 02/25",
            source: "Passiv:Bewohner:Christian R406",
            target: "Passiv:Flurkasse",
            amount: 800,
        },
        {
            valueDate: "04.02.2025",
            description: "Einkauf METRO",
            source: "Passiv:Flurkasse",
            target: "Passiv:Bewohner:Fatih R405",
            amount: 5124,
        },
        {
            valueDate: "01.03.2025",
            description: "Flurbeitrag 03/25",
            source: "Passiv:Bewohner:Yusuf R403",
            target: "Passiv:Flurkasse",
            amount: 800,
        },
        {
            valueDate: "01.03.2025",
            description: "Flurbeitrag 03/25",
            source: "Passiv:Bewohner:Fatih R405",
            target: "Passiv:Flurkasse",
            amount: 800,
        },
        {
            valueDate: "01.03.2025",
            description: "Flurbeitrag 03/25",
            source: "Passiv:Bewohner:Christian R406",
            target: "Passiv:Flurkasse",
            amount: 800,
        },
    ]

    const account: Account = {
        id: 1,
        name: "Passiv:Flurkasse",
        active: false,
    }

    let transactions: Array<DisplayTransaction> = $derived.by(() => {
        let saldo = 0;
        return data.map(transaction => ({
            valueDate: transaction.valueDate,
            description: transaction.description,
            source: transaction.source,
            sourceParts: transaction.source.split(":"),
            target: transaction.target,
            targetParts: transaction.target.split(":"),
            amount: transaction.amount,
            saldo: transaction.target === account.name ? saldo += transaction.amount : saldo -= transaction.amount,
            sign: transaction.target === account.name,
        }));
    })
</script>

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
            {#each transactions as transaction}
                <tr>
                    <td>{transaction.valueDate}</td>
                    <td>{transaction.description}</td>
                    <td>
                        {#if transaction.source === account.name}
                            {#each transaction.targetParts as part, i}
                                {part}
                                {#if i < transaction.targetParts.length - 1}
                                    <Dingsbums/>  
                                {/if}
                            {/each}
                        {:else}
                            {#each transaction.sourceParts as part, i}
                                {part}
                                {#if i < transaction.sourceParts.length - 1}
                                    <Dingsbums/>  
                                {/if}
                            {/each}
                        {/if}
                    </td>
                    {#if transaction.sign}
                        <td class="text-right">{formatEuroCents(transaction.amount)}</td>
                    {:else}
                        <td class="text-right font-semibold text-red-500">- {formatEuroCents(transaction.amount)}</td>                    
                    {/if}
                    {#if transaction.saldo >= 0}
                        <td class="text-right">{formatEuroCents(transaction.saldo)}</td>
                    {:else}
                        <td class="text-right font-semibold text-red-500">{formatEuroCents(transaction.saldo)}</td>                    
                    {/if}
                </tr>
            {/each}
        </tbody>
    </table>
</div>