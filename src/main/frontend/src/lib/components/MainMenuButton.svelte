<script lang="ts">
	import { goto } from '$app/navigation';

	let { redirectTo, children } = $props();

	function onClickFunction() {
		goto(redirectTo);
	}
</script>

<button
	type="button"
	class="h-40 w-40 rounded-lg border border-gray-300 bg-white px-5 py-2.5 text-center text-sm font-medium text-black shadow-sm transition-all hover:bg-gray-100 focus:ring focus:ring-gray-100 disabled:cursor-not-allowed disabled:border-gray-100 disabled:bg-gray-50 disabled:text-gray-400"
    onclick={onClickFunction}
>
	{@render children?.()}
</button>
