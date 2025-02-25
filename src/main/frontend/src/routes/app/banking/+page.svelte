<script module lang="ts">
    export const breadcrumbLabel = "Open Banking";
</script>

<script lang="ts">
	import { goto } from "$app/navigation";

</script>

<div class="inline-flex items-center w-full my-4 gap-2">
	<h1 class="text-2xl flex-1 font-bold pr-2">
		Banking
	</h1>
    <button onclick={() => goto("select-aspsp")} title="Hilfe" class="btn btn-primary h-8 py-0 px-2 m-0">
		Neues Bankkonto Autorisieren
	</button>
</div>
