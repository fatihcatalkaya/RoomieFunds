<script lang="ts">
	import { page } from '$app/state';

	function decideLabel(routeModule: any, pathName: string) {
		if (Object.hasOwn(routeModule, "breadcrumbLabel")) {
			return routeModule["breadcrumbLabel"];
		} else {
			return String(pathName).charAt(0).toUpperCase() + String(pathName).slice(1);
		}
	}

	let crumbs: Array<{ label: string; href: string }> = $derived.by(() => {
		const tokens = page.url.pathname
			.split('/')
			.filter((t) => t !== '');

		let routeModules = import.meta.glob("/src/routes/**/*.svelte", {
			eager: true,
		});

		let completeUrl = "";
		const paths = page.url.pathname.split("/").filter((p) => p != "");

		let _crumbs = [];

		for (let i = 0; i < paths.length; i++) {
			let path = paths[i];
			let previousUrl = completeUrl;
			completeUrl += `/${path}`;

			if (Object.hasOwn(routeModules, "/src/routes" + completeUrl + "/+page.svelte")) {
				_crumbs.push({
					label: decideLabel(routeModules["/src/routes" + completeUrl + "/+page.svelte"], path),
					href: completeUrl
				});
			} else {
				let matchingRoutes = Object.keys(routeModules).filter(route => route.startsWith("/src/routes" + previousUrl + "/["));
				if (matchingRoutes.length != 1) {
					console.warn(`Breadcrumb path ${completeUrl} matches multiple or no routes. Defaulting to capitalized path label.`)
					// console.log(matchingRoutes) // <= uncomment to show what went wrong :)
					_crumbs.push({
						label: String(path).charAt(0).toUpperCase() + String(path).slice(1),
						href: completeUrl
					});
				} else {
					_crumbs.push({
						label: decideLabel(matchingRoutes[0], path),
						href: completeUrl
					});
				}
			}
		}

		return _crumbs;
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
