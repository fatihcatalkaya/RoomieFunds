<script module lang="ts">
	export const breadcrumbLabel = 'Personen';
</script>

<script lang="ts">
	import MdiPencil from '~icons/mdi/pencil';
	import MdiBank from '~icons/mdi/bank';
	import MdiPlus from '~icons/mdi/plus';
	import MdiCheck from '~icons/mdi/check-bold';
	import MdiClose from '~icons/mdi/close-bold';
	import { getApiPerson } from '$lib/client';

	let personQuery = $derived.by(async () => {
		let query = await getApiPerson();

		if (query.error) {
			throw query.error;
		} else {
			return query.data;
		}
	})
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">
		Personen
	</h1>
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
					<td>Name</td>
					<td>Zimmer</td>
					<td>Zahlt Flurbeitrag</td>
					<td>Hat Getränkeliste</td>
					<td class="w-6 text-center">Aktion</td>
				</tr>
			</thead>
			<tbody>
				{#each persons! as person}
					<tr>
						<th>{person.id}</th>
						<td>{person.name}</td>
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