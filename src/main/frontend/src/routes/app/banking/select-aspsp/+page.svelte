<script module lang="ts">
	export const breadcrumbLabel = 'Bank Auswählen';
</script>

<script lang="ts">
	import { type StartAuthorizationDto, type AspspData, type AuthMethod, postApiEnablebanking, getApiEnablebanking } from '$lib/client';

	let nameFilter = $state('');
	let selectedCountry = $state('DE');
	let aspsps = $state<AspspData[]>([]);
	let loading = $state(true);
	let redirecting = $state(false);

	async function fetchAspsps(country: string) {
		loading = true;
		const query = await getApiEnablebanking({ query: { country } });
		if (!query.error && query.data?.aspsps) {
			aspsps = query.data.aspsps;
		}
		loading = false;
	}

	$effect(() => {
		fetchAspsps(selectedCountry);
	});

	let banks = $derived(
		!nameFilter || nameFilter.trim().length == 0
			? aspsps
			: aspsps.filter((aspsp) => aspsp.name?.toLowerCase().includes(nameFilter.toLowerCase()))
	);

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
	let selectedAspsp: AspspData | undefined = $state();
    let selectedAuth: AuthMethod | undefined = $state();

	function selectBank(aspsp: AspspData) {
		selectedAspsp = aspsp;
		bankSelectModal.showModal();
	}

	async function startBankAuthorization(aspsp: AspspData, authMethodName: string) {
		redirecting = true;
        const body: StartAuthorizationDto = {
			aspsp: {
				name: aspsp.name,
				country: aspsp.country
			},
			authMethod: authMethodName,
			maximumConsentValidity: aspsp.maximum_consent_validity
		}

		const query = await postApiEnablebanking({ body });

		if (query.error) {
			console.error(query.error);
			redirecting = false;
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
                    {#each selectedAspsp?.auth_methods!.filter(method => method.psu_type == "PERSONAL") as method}
                        <option value={method}>{method.title ?? method.name}</option>
                    {/each}
                {:else}
                    <option value="" disabled>Fehler :(</option>
                {/if}
            </select>
        </p>
		<div class="modal-action">
			<form method="dialog" class="join">
				<button class="btn join-item">Abbrechen</button>
				<button class="btn btn-primary join-item" onclick={() => startBankAuthorization(selectedAspsp!, selectedAuth?.name!)}>Auswählen</button>
			</form>
		</div>
	</div>
	<form method="dialog" class="modal-backdrop">
		<button>close</button>
	</form>
</dialog>

{#if redirecting}
	<div class="flex flex-col items-center justify-center mt-12 gap-4">
		<span class="loading loading-spinner loading-lg"></span>
		<p class="text-lg">Weiterleitung zur Bank...</p>
	</div>
{:else}
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
			<input type="search" class="grow" bind:value={nameFilter} placeholder="Suche" />
		</label>
	</div>

	<div class="mb-4 flex flex-wrap gap-1">
		{#each countries as country}
			<button
				class="btn btn-sm"
				class:btn-primary={selectedCountry === country.id}
				onclick={() => selectedCountry = country.id}
			>
				{country.name}
			</button>
		{/each}
	</div>

	{#if loading}
		<div class="flex mt-8">
			<span class="loading loading-spinner loading-lg mx-auto"></span>
		</div>
	{:else}
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
							{[...new Set(aspsp?.auth_methods?.map((method) => method.title ?? method.name))].join(', ')}
						</p>
					</div>
				</button>
			{/each}
			{#if banks.length === 0}
				<div class="col-span-full text-center py-8 text-base-content/60">
					Keine Banken gefunden.
				</div>
			{/if}
		</div>
	{/if}
{/if}
