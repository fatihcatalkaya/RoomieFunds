<script module>
	export const breadcrumbLabel = 'Konten';
</script>

<script lang="ts">
	import { type Account } from '$lib/client';
	import AccountListSubtree from '$lib/components/AccountListSubtree.svelte';
	import MdiPlus from '~icons/mdi/plus';
	import MdiScriptText from '~icons/mdi/script-text';
	import type { PageProps } from './$types';

	type TreeNode = {
		name: string;
		children: (TreeNode | Account)[];
	};

	const { data }: PageProps = $props();
	const { accountsQuery } = data;
</script>

<div class="my-4 inline-flex w-full items-center">
	<h1 class="pr-6 text-2xl font-bold">Konten</h1>
	<div class="flex-1 md:flex-0"></div>
	<label class="inline-flex items-center gap-2">
		<input type="checkbox" class="toggle" />
		<span>
			Inaktiv<span class="hidden md:inline">e Konten anzeigen</span>
		</span>
	</label>
	<div class="flex-2"></div>
	<a href="/app/accounts/log" title="Änderungsprotokoll" class="btn btn-primary h-8 w-8 p-0 m-0 mr-2 text-lg">
		<MdiScriptText/>
	</a>
	<a href="/app/accounts/create" class="btn btn-success m-0 h-8 w-8 p-0 text-lg">
		<MdiPlus />
	</a>
</div>

{#await accountsQuery}
	<div>Loading data...</div>
{:then accounts}
	<ul class="menu bg-base-200 rounded-box w-full">
        {#each accounts as tree}
            <AccountListSubtree {...tree} />
        {/each}
	</ul>
{:catch error}
	Error while fetching data!
	<code class="mt-4 block">{error}</code>
{/await}
