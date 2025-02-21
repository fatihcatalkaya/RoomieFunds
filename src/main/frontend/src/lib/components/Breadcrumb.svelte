<script lang="ts">
	import { page } from '$app/state';
	let crumbs: Array<{ label: string; href: string }> = $derived.by(() => {
		const tokens = page.url.pathname
			.replace('/app', '')
			.split('/')
			.filter((t) => t !== '');

		let tokenPath = '';
		let bla = tokens.map((t) => {
			tokenPath += '/' + t;
			t = t.charAt(0).toUpperCase() + t.slice(1);
			return {
				label: page.data.label || t,
				href: '/app' + tokenPath
			};
		});
		// Add a way to get home too.
		bla.unshift({ label: 'Home', href: '/app' });
		return bla;
	});
</script>

<nav aria-label="breadcrumb" class="mt-1 rounded-sm border-1 border-slate-200 p-2">
	<ol class="inline-flex items-center space-x-4 text-sm font-medium">
		{#each crumbs as c, i}
			<li class="inline-flex items-center">
				{#if i == crumbs.length - 1}
					<span class="text-secondary-500 hover:text-secondary-600">
						{c.label}
					</span>
				{:else}
					<a href={c.href} class="text-secondary-500 hover:text-secondary-600">{c.label}</a>
				{/if}
			</li>

			{#if i != crumbs.length - 1}
				<li class="inline-flex items-center space-x-4">
					<svg
						class="h-6 w-6 text-gray-400"
						fill="currentColor"
						viewBox="0 0 20 20"
						xmlns="http://www.w3.org/2000/svg"
					>
						<path
							fill-rule="evenodd"
							d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"
							clip-rule="evenodd"
						></path>
					</svg>
				</li>
			{/if}
		{/each}
	</ol>
</nav>
