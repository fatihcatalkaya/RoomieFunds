<script module lang="ts">
	export const breadcrumbLabel = 'Produkte';
</script>

<script lang="ts">
	import { formatEuroCents } from '$lib/formatter';
	import MdiPencil from '~icons/mdi/pencil';
	import MdiPlus from '~icons/mdi/plus';
	import { type Product } from '$lib/client';
	import type { PageProps } from './$types';

	let { data }: PageProps = $props();
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">
		Produkte
	</h1>
	<a href="/app/products/create" class="btn btn-success h-8 w-8 p-0 m-0 text-lg">
		<MdiPlus/>
	</a>
</div>

<div class="rounded-box border-base-content/5 bg-base-100 overflow-x-auto border border-slate-300 px-0 mx-0">
	<table class="table table-zebra">
		<thead>
			<tr>
				<th>ID</th>
				<th>Name</th>
				<th class="text-right">Preis</th>
				<th class="w-6 text-center">Action</th>
			</tr>
		</thead>
		<tbody>
			{#each data.products ?? [] as product}
				<tr>
					<th>{product.id}</th>
					<td>{product.name}</td>
					<td class="text-right">{formatEuroCents(product.price!)}</td>
					<td class="max-w-4 text-center">
						<a href="/app/products/edit/{product.id}" class="btn btn-primary h-8 w-8 p-0 m-0 text-lg">
							<MdiPencil />
						</a>
					</td>
				</tr>
			{/each}
		</tbody>
	</table>
</div>
