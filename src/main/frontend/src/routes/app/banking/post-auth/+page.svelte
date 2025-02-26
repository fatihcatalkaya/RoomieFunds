<script module lang="ts">
    export const breadcrumbLabel = "Authentifizierungsfluss";
</script>

<script lang="ts">
	import { page } from "$app/state";

    const state = page.url.searchParams.get("state")
</script>

{#if !state}
    <div></div>
{:else if state == "success"}
    <p class="mb-4">
        Die Authentifizierung war erfolgreich.
    </p>

    <a class="btn" href="/app/banking">Zurück zur Banking-Oberfläche</a>
{:else if state == "failed"}
    <p class="mb-4">
        Die Authentifizierung ist fehlgeschlagen.
    </p>

    <a class="btn" href="/app/banking">Zurück zur Banking-Oberfläche</a>
{:else}
    huh?
{/if}