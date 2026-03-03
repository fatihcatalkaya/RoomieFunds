<script module lang="ts">
    export const breadcrumbLabel = "Authentifizierungsfluss";
</script>

<script lang="ts">
	import { goto } from "$app/navigation";
	import { page } from "$app/state";

    const state = page.url.searchParams.get("state");

	$effect(() => {
		if (state === "success") {
			const timeout = setTimeout(() => {
				goto("/app/banking");
			}, 3000);
			return () => clearTimeout(timeout);
		}
	});
</script>

{#if !state}
    <div></div>
{:else if state == "success"}
    <div role="alert" class="alert alert-success my-4">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 shrink-0 stroke-current" fill="none" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>Die Authentifizierung war erfolgreich. Du wirst in Kürze weitergeleitet...</span>
    </div>

    <a class="btn" href="/app/banking">Zurück zur Banking-Oberfläche</a>
{:else if state == "failed"}
    <div role="alert" class="alert alert-error my-4">
        <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 shrink-0 stroke-current" fill="none" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <span>Die Authentifizierung ist fehlgeschlagen.</span>
    </div>

    <a class="btn" href="/app/banking">Zurück zur Banking-Oberfläche</a>
{:else}
    <div role="alert" class="alert alert-warning my-4">
        <span>Unbekannter Status.</span>
    </div>

    <a class="btn" href="/app/banking">Zurück zur Banking-Oberfläche</a>
{/if}
