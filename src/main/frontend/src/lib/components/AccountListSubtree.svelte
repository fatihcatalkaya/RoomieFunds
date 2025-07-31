<script lang="ts">
	import { formatEuroCents } from '$lib/formatter';
	import Self from './AccountListSubtree.svelte';
	const { children, name } = $props();
</script>

<li>
	<h2 class="menu-title text-neutral">{name}</h2>
	<ul>
		{#each children as child}
			{#if Object.hasOwn(child, 'active')}
				<li>
					<a href={'/app/accounts/transactions/' + child.id}
						>{child.name.split(':').pop()}
						<span class="text-right font-bold" class:text-red-500={child.balance < 0}
							>{formatEuroCents(child.balance)}</span
						></a
					>
				</li>
			{:else}
				<Self {...child} />
			{/if}
		{/each}
	</ul>
</li>
