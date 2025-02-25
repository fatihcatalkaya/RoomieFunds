<script module lang="ts">
    export const breadcrumbLabel = "Striche zählen";
</script>

<script lang="ts">
	import { goto } from "$app/navigation";

	import { getApiPerson, postApiTransaction, type Account, type Person, type Product } from "$lib/client";
	import { formatEuroCents } from "$lib/formatter";
	import { error } from "@sveltejs/kit";

    const { data }: { data: { products: Product[], mainAccount: Account } } = $props();
    const products = data.products;
    const mainAccount = data.mainAccount;

    interface TallyData {
        [key: string]: {
            product: Product,
            count: number,
        }
    }

    let tallyData: TallyData = $state(Object.fromEntries(products.map(product => [product.id, { product, count: 0 }])));
    let sum = $derived(Object.entries(tallyData).map(entry => entry[1].count * entry[1].product.price!).reduce((acc, value) => acc + value, 0))

    let personQuery = $derived.by(async () => {
        const query = await getApiPerson();

        if (query.error) {
            throw error;
        } else {
            return query.data?.filter(person => person.printOnProductTallyList)
        }
    });

    let selectedPerson: Person | undefined = $state();
    let confirmModal: HTMLDialogElement;
    let errorAlert: HTMLDivElement;

    async function submitTransaction() {
        const query = await postApiTransaction({
            body: {
                amount: sum,
                description: "Strichlistenzählung",
                sourceAccountId: selectedPerson?.accountId,
                targetAccountId: mainAccount.id,
                valueDate: new Date(Date.now()).toISOString().substring(0, 10),
            }
        });

        if (query.error) {
            console.error(error);
            errorAlert.hidden = false;   
        } else {
            goto("../")
        }
    }
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">
		Strichliste zählen
	</h1>
</div>

<dialog class="modal" bind:this={confirmModal}>
	<div class="modal-box">
		<h3 class="text-lg font-bold">Buchung für {selectedPerson?.name} eintragen?</h3>
		<p class="py-4">{selectedPerson?.name} bekommt {formatEuroCents(sum)} von seinem Konto abgebucht.</p>
		<div class="modal-action">
			<form method="dialog" class="join">
                <button class="btn join-item">Abbrechen</button>
				<button class="btn btn-success join-item" onclick={submitTransaction}>Eintragen</button>
			</form>
		</div>
	</div>
	<form method="dialog" class="modal-backdrop">
		<button>close</button>
	</form>
</dialog>

<div role="alert" class="alert alert-error mb-4" bind:this={errorAlert} hidden>
    <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 shrink-0 stroke-current" fill="none" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
    <span>Die Buchung konnte nicht eingetragen werden!</span>
</div>

<form class="mx-auto max-w-md" onsubmit={() => confirmModal.showModal()}>
    <label class="flex gap-2 items-center">
        <span class="flex-1">Person</span>
        <select class="select flex-2" bind:value={selectedPerson} required>
            {#await personQuery}
                <option value="" disabled>Loading persons...</option>
            {:then personList}
                {#each personList! as person}
                    <option value="{person}">{person.name} {person.room}</option>
                {/each}
            {:catch}
                <option value="" disabled>Error fetching persons</option>
            {/await}
        </select>
    </label>
    {#each Object.entries(tallyData) as [productId, tally]}
        <label class="flex gap-2 items-center mt-2">
            <span class="flex-2">{tally.product.name}</span>
            <input type="number" class="input flex-1" bind:value={tallyData[productId].count} min="0" step="1">
        </label>
    {/each}
    <hr class="mt-4 text-base-300">
    <div class="mt-4 flex gap-2 items-center font-bold pr-4">
        <span class="flex-1">Summe</span>
        <span class="text-right">{formatEuroCents(sum)}</span>
    </div>
    <div class="mt-4 join grid grid-cols-2">
        <button class="btn order-1 btn-primary join-item">Bestätigen</button>
        <button class="btn order-0 join-item" onclick={event => { event.preventDefault(); goto("../") }}>Abbrechen</button>
    </div>
</form>