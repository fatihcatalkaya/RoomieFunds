<script module>
	export const breadcrumbLabel = 'Konten';
</script>

<script lang="ts">
	import { getApiAccount, type Account } from '$lib/client';
	import MdiPlus from '~icons/mdi/plus';
	import AccountListSubtree from '$lib/components/AccountListSubtree.svelte';

	type TreeNode = {
		name: string;
		children: (TreeNode | Account)[];
	};

	function buildTree(accounts: Account[]): TreeNode[] {
		const rootNodes: TreeNode[] = [];

		// Function to insert an account into the tree
		const insertToTree = (path: string[], account: Account, node: TreeNode[]) => {
			const segment = path.shift();
			if (!segment) return;

			// Find if the node with the same name already exists
			let currentNode = node.find((n) => n.name === segment) as TreeNode | undefined;

			if (!currentNode) {
				// If the node doesn't exist, create a new one
				currentNode = { name: segment, children: [] };
				node.push(currentNode);
			}

			// If there are still parts of the path, continue building the tree
			if (path.length > 1) {
				insertToTree(path, account, currentNode.children as any);
			} else {
				// When we reach the last segment, insert the account as a leaf node
				// Instead of pushing an Account directly, we push a TreeNode with children
				currentNode.children.push(account);
			}
		};

		// Iterate through all accounts and insert them into the tree
		accounts.forEach((account) => {
			const path = account.name!.split(':');
			insertToTree(path, account, rootNodes);
		});

        console.log(rootNodes);
		return rootNodes;
	}

	let accounts = $derived.by(async () => {
		let accountQuery = await getApiAccount();

		if (accountQuery.error) {
			throw accountQuery.error;
		} else {
			return buildTree(accountQuery.data!);
		}
	});
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
	<a href="/app/accounts/create" class="btn btn-success m-0 h-8 w-8 p-0 text-lg">
		<MdiPlus />
	</a>
</div>

{#await accounts}
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
