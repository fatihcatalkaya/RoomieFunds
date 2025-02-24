<script lang="ts">
	import { page } from '$app/state';

	function decideLabel(routeModule: any, pathName: string) {
		if (routeModule && Object.hasOwn(routeModule, "breadcrumbLabel")) {
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
		let completeRoute = "/src/routes";
		const paths = page.url.pathname.split("/").filter((p) => p != "");

		let _crumbs = [];

		for (let i = 0; i < paths.length; i++) {
			let path = paths[i];
			let previousUrl = completeUrl;
			completeUrl += `/${path}`;

			if (Object.hasOwn(routeModules, completeRoute + "/+page.svelte")) {
				completeRoute += `/${path}`
				_crumbs.push({
					label: decideLabel(routeModules["/src/routes" + completeUrl + "/+page.svelte"], path),
					href: completeUrl
				});
			} else {
				let matchingRoutes = Object.keys(routeModules).filter(route => route.startsWith(completeRoute + "/["));
				if (matchingRoutes.length != 1) {
					console.warn(`Breadcrumb path ${completeUrl} matches multiple or no routes. Defaulting to capitalized path label.`)
					// console.log(matchingRoutes) // <= uncomment to show what went wrong :)
					_crumbs.push({
						label: String(path).charAt(0).toUpperCase() + String(path).slice(1),
						href: completeUrl
					});
				} else {
					let string = matchingRoutes[0].replace("/src/routes" + previousUrl + "/", "").split("/")[0]
					completeRoute += `/${string}`
					_crumbs.push({
						label: decideLabel(routeModules[completeRoute + "/+page.svelte"], path),
						href: completeUrl
					});
				}
			}
		}

		return _crumbs;
	});
</script>

<nav aria-label="breadcrumb" class="px-4 py-2 mb-2 mt-4 rounded-box border-base-content/5 bg-base-100 border border-slate-300 overflow-x-scroll">
	<ol class="inline-flex items-center space-x-4 text-sm font-medium text-nowrap after:w-4">
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
