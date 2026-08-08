<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import {
		postApiEnablebankingSessionUnfinishedBySessionId,
		type Account,
		type EnableBankingAccountDto
	} from '$lib/client';
	import ErrorAlert from '$lib/components/ErrorAlert.svelte';
	import type { PageProps } from './$types';

	const { data }: PageProps = $props();
	const { unfinishedSessionQuery, accountsQuery } = data.streamed;

	let bankAccount: EnableBankingAccountDto | undefined = $state();
	let roomieFundsAccount: Account | undefined = $state();
	let errorState = $state(false);

	async function completeSession() {
		const query = await postApiEnablebankingSessionUnfinishedBySessionId({
			path: {
				sessionId: Number(page.params.intSessionId)
			},
			body: {
				accountId: roomieFundsAccount?.id!,
				bankAccountIban: bankAccount?.iban!,
				bankAccountUid: bankAccount?.uid!
			}
		});

		if (query.error) {
			errorState = true;
			console.error(query.error);
		} else {
			goto('/app/banking');
		}
	}
</script>

{#if errorState}
	<ErrorAlert>Die Kontodaten konnten nicht vervollständigt werden!</ErrorAlert>
{/if}

{#await unfinishedSessionQuery}
	<div class="mt-4 flex">
		<span class="loading loading-spinner loading-lg mx-auto"></span>
	</div>
{:then unfinishedSession}
	<div class="my-4 inline-flex w-full items-center gap-2">
		<h1 class="flex-1 pr-2 text-2xl font-bold">Konto auswählen</h1>
	</div>

	<p class="mb-4">Achtung: Diese Aktion kann nicht Rückgängig gemacht werden!</p>

	{#await accountsQuery}
		<span class="loading loading-spinner loading-md mx-auto"></span>
	{:then roomieFundsAccounts}
		<form onsubmit={completeSession}>
			<fieldset class="fieldset">
				<legend class="fieldset-legend">Bankkonto</legend>
				<select class="select" bind:value={bankAccount} required>
					{#each unfinishedSession.accounts! as account}
						<option value={account}>{account.iban}</option>
					{/each}
				</select>
			</fieldset>
			<fieldset class="fieldset">
				<legend class="fieldset-legend">Verknüpftes Konto</legend>
				<select class="select" bind:value={roomieFundsAccount} required>
					{#each roomieFundsAccounts as account}
						<option value={account}>{account.name}</option>
					{/each}
				</select>
			</fieldset>
			<div class="mt-4 grid grid-cols-2">
				<button class="btn btn-primary order-1">JUP</button>
				<a href="/app/banking" class="btn order-0">nö</a>
			</div>
		</form>
	{/await}
{:catch}
	<ErrorAlert>Die Sitzung {page.params.intSessionId} kann nicht vervollständigt werden!</ErrorAlert>
{/await}
