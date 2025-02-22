<script module lang="ts">
	export const prerender = false;
	export const breadcrumbLabel = 'Personen';
</script>

<script lang="ts">
	import { formatEuroCents } from '$lib/formatter';
	import MdiPencil from '~icons/mdi/pencil';
	import MdiPlus from '~icons/mdi/plus';
	import MdiCheck from '~icons/mdi/check-bold';
	import MdiClose from '~icons/mdi/close-bold';

	type Person = {
		id: number;
		name: string;
		room: string;
		paysFloorFees: boolean;
	}

	let persons: Array<Person> = $derived([
		{
			id: 1,
			name: "Fatih",
			room: "R405",
			paysFloorFees: true,
		},
		{
			id: 2,
			name: "Christian",
			room: "R406",
			paysFloorFees: true,
		},
		{
			id: 3,
			name: "M. Mustermann",
			room: "R420",
			paysFloorFees: false,
		},
		{
			id: 4,
			name: "M. Musterfrau",
			room: "R496",
			paysFloorFees: false,
		},
	]);
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">
		Personen
	</h1>
	<a href="/app/persons/create" class="btn btn-success h-8 w-8 p-0 m-0 text-lg">
		<MdiPlus/>
	</a>
</div>

<div class="rounded-box border-base-content/5 bg-base-100 overflow-x-auto border border-slate-300 px-0 mx-0">
	<table class="table table-zebra text-nowrap">
		<thead>
			<tr>
				<th>ID</th>
				<td>Name</td>
				<td>Zimmer</td>
				<td>Zahlt Flurbeitrag</td>
				<td class="w-6 text-center">Aktion</td>
			</tr>
		</thead>
		<tbody>
			{#each persons as person}
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
					<td class="max-w-4 text-center">
						<a href="/app/persons/edit/{person.id}" class="btn btn-primary h-8 w-8 p-0 m-0 text-lg">
							<MdiPencil />
						</a>
					</td>
				</tr>
			{/each}
		</tbody>
	</table>
</div>
