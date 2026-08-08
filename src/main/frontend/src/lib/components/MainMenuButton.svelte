<script lang="ts">
	import { goto } from '$app/navigation';

	let { redirectTo, children } = $props();
</script>

<a
	class="btn bg-base-100 flex h-40 w-40 flex-col rounded-xl text-2xl font-normal shadow-md"
	href={redirectTo}
>
	{@render children?.()}
</a>
