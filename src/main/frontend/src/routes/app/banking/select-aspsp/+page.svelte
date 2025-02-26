<script module lang="ts">
	export const breadcrumbLabel = 'Bank Auswählen';
</script>

<script lang="ts">
	import { goto } from '$app/navigation';

	import { aspsps, type ASPSP, type AuthMethod } from '$lib/aspsps';
	import { postApiTest, type StartAuthorizationDto } from '$lib/client';

	let nameFilter = $state('');
	let banks = $derived(
		!nameFilter || nameFilter.trim().length == 0
			? aspsps
			: aspsps.filter((aspsp) => aspsp.name.includes(nameFilter))
	);

	console.log(aspsps.filter((aspsp) => aspsp.name === 'Volksbank pur'));

	const countries = [
		{ id: 'DE', name: 'Deutschland' },
		{ id: 'AT', name: 'Österreich' },
		{ id: 'BE', name: 'Belgien' },
		{ id: 'BG', name: 'Bulgarien' },
		{ id: 'HR', name: 'Kroatien' },
		{ id: 'CY', name: 'Zypern' },
		{ id: 'CZ', name: 'Tschechein' },
		{ id: 'DK', name: 'Dänemark' },
		{ id: 'EE', name: 'Estland' },
		{ id: 'FI', name: 'Finland' },
		{ id: 'FR', name: 'Frankreich' },
		{ id: 'GR', name: 'Griechenland' },
		{ id: 'HU', name: 'Ungarn' },
		{ id: 'IS', name: 'Island' },
		{ id: 'IE', name: 'Irland' },
		{ id: 'IT', name: 'Italien' },
		{ id: 'LV', name: 'Lettland' },
		{ id: 'LT', name: 'Litauen' },
		{ id: 'LU', name: 'Luxemburg' },
		{ id: 'MT', name: 'Malta' },
		{ id: 'NL', name: 'Niederlande' },
		{ id: 'NO', name: 'Norwegen' },
		{ id: 'PL', name: 'Polen' },
		{ id: 'PT', name: 'Portugal' },
		{ id: 'RO', name: 'Rumänien' },
		{ id: 'SK', name: 'Slowakei' },
		{ id: 'SI', name: 'Slowenien' },
		{ id: 'ES', name: 'Spanien' },
		{ id: 'SE', name: 'Schweden' }
	];

	let bankSelectModal: HTMLDialogElement;
	let selectedAspsp: ASPSP | undefined = $state();
    let selectedAuth: AuthMethod | undefined = $state();

	function selectBank(aspsp: ASPSP) {
		selectedAspsp = aspsp;
		bankSelectModal.showModal();
	}

	async function selectBankAndDingsbumd(aspsp: ASPSP, authMethodName: string) {
        const body: StartAuthorizationDto = {
			aspsp: {
				name: aspsp.name,
				country: aspsp.country
			},
			authMethod: authMethodName,
			maximumConsentValidity: aspsp.maximum_consent_validity
		}

		const query = await postApiTest({ body });

		if (query.error) {
			console.error(query.error)
		} else {
			let data = query.data!;
			window.location = data.url! as any;
		}
    }
</script>

<dialog class="modal" bind:this={bankSelectModal}>
	<div class="modal-box">
		<h3 class="text-lg font-bold">{selectedAspsp?.name}</h3>
		<p class="py-4 flex flex-wrap gap-2">
            Wähle deine bevorzugte Authentifizierungsmethode aus:
            <select class="select" bind:value={selectedAuth}>
                {#if selectedAspsp}
                    {#each selectedAspsp!.auth_methods.filter(method => method.psu_type == "PERSONAL") as method}
                        <option value="{method}">{method.title ?? method.name}</option>
                    {/each}
                {:else}
                    <option value="" disabled>Fehler :(</option>
                {/if}
            </select>
        </p>
		<div class="modal-action">
			<form method="dialog" class="join">
				<button class="btn join-item">Abbrechen</button>
				<button class="btn btn-primary join-item" onclick={() => selectBankAndDingsbumd(selectedAspsp!, selectedAuth!.name)}>Auswählen</button>
			</form>
		</div>
	</div>
	<form method="dialog" class="modal-backdrop">
		<button>close</button>
	</form>
</dialog>

<div class="flex w-full items-center">
	<div class="my-4 flex-1 text-2xl font-bold">Bank</div>
	<label class="input">
		<svg class="h-[1em] opacity-50" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
			><g
				stroke-linejoin="round"
				stroke-linecap="round"
				stroke-width="2.5"
				fill="none"
				stroke="currentColor"
				><circle cx="11" cy="11" r="8"></circle><path d="m21 21-4.3-4.3"></path></g
			></svg
		>
		<input type="search" class="grow" bind:value={nameFilter} placeholder="Search" />
		<kbd class="kbd kbd-sm">⌘</kbd>
		<kbd class="kbd kbd-sm">K</kbd>
	</label>
</div>

<form class="mb-4 filter">
	<input class="btn btn-square" type="reset" value="×" />
	{#each countries as country}
		<input class="btn" type="radio" name="frameworks" aria-label={country.name} />
	{/each}
</form>

<div class="grid grid-cols-2 gap-2 lg:grid-cols-5 lg:gap-4">
	{#each banks as aspsp}
		<button
			onclick={() => selectBank(aspsp)}
			class="card flex-dir-cols bg-base-100 hover:bg-base-200 transform cursor-pointer shadow-sm hover:shadow-2xl focus:ring"
		>
			<figure class="flex-grow px-4 pt-4">
				<img src={aspsp.logo} alt="Logo {aspsp.name}" class="rounded-xl" />
			</figure>
			<div class="card-body flex-shrink items-center justify-end text-center">
				<h2 class="card-title">{aspsp.name}</h2>
				<p>
					<strong>Unterstützt:</strong>
					{[...new Set(aspsp.auth_methods.map((method) => method.title ?? method.name))].join(', ')}
				</p>
			</div>
		</button>
	{/each}
</div>
