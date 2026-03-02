<script module lang="ts">
	export const breadcrumbLabel = 'Personen';
</script>

<script lang="ts">
	import MdiPencil from '~icons/mdi/pencil';
	import MdiBank from '~icons/mdi/bank';
	import MdiPlus from '~icons/mdi/plus';
	import MdiCheck from '~icons/mdi/check-bold';
	import MdiClose from '~icons/mdi/close-bold';
	import MdiEmailFast from '~icons/mdi/email-fast';
	import MdiScriptText from '~icons/mdi/script-text';
	import type { PageProps } from './$types';
	import { getApiAccountSendAccountStatementsNow } from '$lib/client';

	let sendAccountStatementsSuccess = $state(false);
	let emailSendModal: HTMLDialogElement;
	const { data }: PageProps = $props();
	const { personQuery } = data.streamed

	async function sendAccountStatementsNow() {
		const response = await getApiAccountSendAccountStatementsNow();
		sendAccountStatementsSuccess = !response.error;
		emailSendModal.showModal();
	}
</script>

<dialog id="emailSendModal" class="modal" bind:this={emailSendModal}>
	<div class="modal-box">
	  <form method="dialog">
		<button class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2">✕</button>
	  </form>
	  <h3 class="text-lg font-bold">Nachricht</h3>
	  <p class="py-4">
		{#if sendAccountStatementsSuccess}
			Der E-Mail-Versand wurde erfolgreich gestartet.
		{:else}
			Beim Versenden der E-Mails ist ein Fehler aufgetreten!
		{/if}
	  </p>
	</div>
	<form method="dialog" class="modal-backdrop">
	  <button>close</button>
	</form>
</dialog>

<div class="inline-flex items-center w-full my-4 gap-1">
	<h1 class="flex-grow text-2xl font-bold">
		Personen
	</h1>
	<button class="btn btn-warning h-8 w-8 p-0 m-0 text-lg" onclick={sendAccountStatementsNow}>
		<MdiEmailFast />
	</button>
	<a href="/app/persons/log" title="Änderungsprotokoll" class="btn btn-primary h-8 w-8 p-0 m-0 text-lg">
		<MdiScriptText/>
	</a>
	<a href="/app/persons/create" title="Person Erstellen" class="btn btn-success h-8 w-8 p-0 m-0 text-lg">
		<MdiPlus/>
	</a>
</div>

{#await personQuery}
	Loading persons...
{:then persons}
	<div class="rounded-box border-base-content/5 bg-base-100 overflow-x-auto border border-slate-300 px-0 mx-0">
		<table class="table table-zebra text-nowrap">
			<thead>
				<tr>
					<th>ID</th>
					<td>Vorname</td>
					<td>Nachname</td>
					<td>E-Mail</td>
					<td>Zimmer</td>
					<td>Zahlt Flurbeitrag</td>
					<td>Hat Getränkeliste</td>
					<td>Konto-Auszug per E-Mail</td>
					<td class="w-6 text-center">Aktion</td>
				</tr>
			</thead>
			<tbody>
				{#each persons! as person}
					<tr>
						<th>{person.id}</th>
						<td>{person.firstName}</td>
						<td>{person.lastName}</td>
						<td>{person.email}</td>
						<td>{person.room}</td>
						<td>
							{#if person.paysFloorFees}
								<span class="text-success"><MdiCheck/></span>
							{:else}
								<span class="text-error"><MdiClose/></span>
							{/if}
						</td>
						<td>
							{#if person.printOnProductTallyList}
								<span class="text-success"><MdiCheck/></span>
							{:else}
								<span class="text-error"><MdiClose/></span>
							{/if}
						</td>
						<td>
							{#if person.emailAccountStatement}
								<span class="text-success"><MdiCheck/></span>
							{:else}
								<span class="text-error"><MdiClose/></span>
							{/if}
						</td>
						<td class="text-center">
							<a href="/app/persons/edit/{person.id}" title="Person {person.id} bearbeiten" class="btn btn-primary h-8 w-8 p-0 m-0 text-lg">
								<MdiPencil />
							</a>
							<a href="/app/accounts/transactions/{person.accountId}" title="Person {person.id} bearbeiten" class="btn btn-secondary h-8 w-8 p-0 m-0 text-lg">
								<MdiBank />
							</a>
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>	
{:catch error}
	Error while fetching persons!
	<pre>{JSON.stringify(error, null, 2)}</pre>
{/await}