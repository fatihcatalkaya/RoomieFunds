<script module lang="ts">
	export const breadcrumbLabel = 'Produkte';
</script>

<script lang="ts">
	import { formatEuroCents } from '$lib/formatter';
	import MdiPencil from '~icons/mdi/pencil';
	import MdiPlus from '~icons/mdi/plus';
	import MdiScriptText from '~icons/mdi/script-text';
	import MdiPrinter from '~icons/mdi/printer';
	
	import type { PageProps } from './$types';
	import { getApiProductTallylist } from '$lib/client';

	let { data }: PageProps = $props();

	async function printTallylist() {
		let query = await getApiProductTallylist();
		openPDFInNewTab(query.data as Blob)
	}

	// Assuming arrayBuffer is already available
	function openPDFInNewTab(blob: Blob) {
		const url = URL.createObjectURL(blob);
		const newTab = window.open(url, '_blank')!;
		newTab.onbeforeunload = () => {
			URL.revokeObjectURL(url);
		};
	}
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">
		Produktliste
	</h1>
	<button title="Änderungsprotokoll" class="btn btn-warning h-8 w-8 p-0 m-0 mr-2 text-lg" onclick={printTallylist}>
		<MdiPrinter/>
	</button>
	<a href="/app/products/log" title="Änderungsprotokoll" class="btn btn-primary h-8 w-8 p-0 m-0 mr-2 text-lg">
		<MdiScriptText/>
	</a>
	<a href="/app/products/create" title="Neues Produkt" class="btn btn-success h-8 w-8 p-0 m-0 text-lg">
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
						<a href="/app/products/edit/{product.id}" title="Produkt {product.id} bearbeiten" class="btn btn-primary h-8 w-8 p-0 m-0 text-lg">
							<MdiPencil />
						</a>
					</td>
				</tr>
			{/each}
		</tbody>
	</table>
</div>
