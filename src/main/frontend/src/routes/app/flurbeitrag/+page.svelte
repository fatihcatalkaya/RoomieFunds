<script module lang="ts">
	export const breadcrumbLabel = 'Flurbeitrag';
</script>

<script lang="ts">
	import { putApiFlurbeitrag, putApiKontoFlurkonto } from '$lib/client';
	import EuroInput from '$lib/components/EuroInput.svelte';
	import ErrorAlert from '$lib/components/ErrorAlert.svelte';
	import type { PageProps } from './$types';

	const { data }: PageProps = $props();
	const {
		flurbeitrag: flurbeitragQuery,
		flurkonto: flurkontoQuery,
		accounts: accountsQuery
	} = data;

	const loadError = flurbeitragQuery.error ?? flurkontoQuery.error ?? accountsQuery.error;
	const accounts = accountsQuery.data ?? [];
	const initialAmount = flurbeitragQuery.data?.flurbeitrag ?? 0;
	// Undefined until a Flurkonto has ever been configured — the scheduler skips the
	// monthly booking in that case, so the field starts on a disabled placeholder.
	const initialFlurkontoId = flurkontoQuery.data?.id;

	// EuroInput hands back null once the field is cleared, so the bound value is nullable.
	let amount: number | null = $state(initialAmount);
	let flurkontoId: number | undefined = $state(initialFlurkontoId);

	let saving = $state(false);
	let saveError: string | null = $state(null);

	// What the backend last confirmed. Comparing against the fields keeps the
	// success banner up only as long as the form still shows the saved state, and
	// lets the save skip endpoints whose value did not change — every write is
	// logged, so re-sending an unchanged value would only add noise.
	let savedAmount = $state(initialAmount);
	let savedFlurkontoId: number | undefined = $state(initialFlurkontoId);
	let justSaved = $state(false);
	const saved = $derived(
		justSaved && (amount ?? 0) === savedAmount && flurkontoId === savedFlurkontoId
	);

	// The endpoints are restricted to roomiefunds-admin, and the menu tile is shown
	// to everyone, so a 403 is the expected failure for normal users.
	function describeError(status: number, subject: string) {
		return status == 403
			? 'Keine Berechtigung: Zum Ändern des Flurbeitrags wird die Rolle roomiefunds-admin benötigt.'
			: `${subject} konnte nicht gespeichert werden.`;
	}

	async function saveFlurbeitrag() {
		const value = amount ?? 0;

		saveError = null;
		justSaved = false;

		// `allowNegative` only turns off EuroInput's own sign handling — the
		// underlying number input still keeps a minus that was typed into it. So
		// the negative case is caught here rather than relying on the field, which
		// also spares the backend a request it would answer with 400
		// (@PositiveOrZero on the Flurbeitrag record).
		if (value < 0) {
			saveError = 'Der Flurbeitrag darf nicht negativ sein.';
			return false;
		}

		saving = true;

		if (value !== savedAmount) {
			const query = await putApiFlurbeitrag({ body: { flurbeitrag: value } });

			if (query.error) {
				console.error(query.error);
				saveError = describeError(query.response.status, 'Der Flurbeitrag');
				saving = false;
				return false;
			}

			// Normalizes a cleared field to 0,00 €, matching what was just stored.
			amount = value;
			savedAmount = value;
		}

		if (flurkontoId !== undefined && flurkontoId !== savedFlurkontoId) {
			// The endpoint takes a bare number, for which the generator emits
			// `bodySerializer: null`. The client only ever sends `serializedBody`, so
			// without a serializer the request goes out with no body at all and the
			// backend answers 415. Stringifying it here keeps the generated
			// `Content-Type: text/plain` header meaningful.
			const query = await putApiKontoFlurkonto({
				body: flurkontoId,
				bodySerializer: (id: number) => String(id)
			});

			if (query.error) {
				console.error(query.error);
				saveError = describeError(query.response.status, 'Das Flurkonto');
				saving = false;
				return false;
			}

			savedFlurkontoId = flurkontoId;
		}

		justSaved = true;
		saving = false;

		return false;
	}
</script>

<h1 class="my-4 text-2xl font-bold">Flurbeitrag</h1>

{#if loadError}
	<ErrorAlert>Konnte die Flurbeitrag-Einstellungen nicht laden!</ErrorAlert>
	<code class="mt-4 w-full">
		{JSON.stringify(loadError, null, 2)}
	</code>
{:else}
	{#if saved}
		<div role="alert" class="alert alert-success mt-4">
			<svg
				xmlns="http://www.w3.org/2000/svg"
				class="h-6 w-6 shrink-0 stroke-current"
				fill="none"
				viewBox="0 0 24 24"
			>
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					stroke-width="2"
					d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
				/>
			</svg>
			<span>Flurbeitrag gespeichert.</span>
		</div>
	{/if}

	{#if saveError}
		<ErrorAlert>{saveError}</ErrorAlert>
	{/if}

	<p class="mx-auto mt-4 max-w-md">
		Der Flurbeitrag wird jeden Monat automatisch allen Personen abgebucht, die Flurbeitrag bezahlen,
		und dem Flurkonto gutgeschrieben.
	</p>

	<form method="dialog" class="mx-auto mt-4 grid max-w-md grid-cols-1 gap-2">
		<label class="flex w-full items-center">
			<span class="w-1/4">Betrag</span>
			<EuroInput class="input w-3/4" bind:value={amount} />
		</label>
		<label class="flex w-full items-center">
			<span class="w-1/4">Flurkonto</span>
			<select class="select w-3/4" bind:value={flurkontoId}>
				<option value={undefined} disabled>Kein Flurkonto ausgewählt</option>
				{#each accounts as account (account.id)}
					<option value={account.id}>{account.name}</option>
				{/each}
			</select>
		</label>
		<div class="join mt-2 w-full">
			<a href="/app" class="join-item btn btn-warn w-1/2"> Zurück </a>
			<input
				class="join-item btn btn-success w-1/2"
				value="Speichern"
				type="submit"
				disabled={saving}
				onclick={saveFlurbeitrag}
			/>
		</div>
	</form>
{/if}
