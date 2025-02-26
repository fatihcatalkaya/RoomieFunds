<script lang="ts">
	import { goto } from '$app/navigation';

	let { redirectTo, children } = $props();
</script>

<a
	class="h-40 w-40 btn rounded-xl bg-base-100 shadow-md font-normal text-2xl flex flex-col"
	href={redirectTo}
>
	{@render children?.()}
</a>
