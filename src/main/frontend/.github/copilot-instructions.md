# RoomieFunds Frontend - AI Coding Instructions

## Project Overview

SvelteKit 2 SPA for shared living expense management (roommate accounting, drinks tally, bank integration). German-language UI with a Quarkus backend.

## Tech Stack & Key Dependencies

- **Framework**: SvelteKit 2 with Svelte 5 runes (`$state`, `$derived`, `$props`, `$effect`)
- **Styling**: Tailwind CSS 4 + DaisyUI 5 component library
- **Icons**: `unplugin-icons` with `~icons/mdi/` prefix (Material Design Icons)
- **API Client**: Auto-generated via `@hey-api/openapi-ts` from backend OpenAPI spec
- **Auth**: `oidc-spa` with Keycloak OIDC provider
- **Build**: Static SPA output (`adapter-static`), no SSR

## Critical Developer Commands

```bash
npm run dev                    # Start dev server (proxies /api to :8080, /realms to :9090)
npm run openapi-ts             # Regenerate API client (requires backend on localhost:8080)
npm run check                  # TypeScript + Svelte type checking
npm run lint && npm run format # Prettier + ESLint
```

## API Client Pattern

All API types/functions are generated in `src/lib/client/`. Never edit these files manually.

```typescript
// Import from the barrel export
import { getApiPerson, postApiTransaction, type Person } from '$lib/client';

// All SDK functions return { data, error } pattern
const query = await getApiPerson();
if (query.error) {
	throw query.error;
}
return query.data;
```

Auth token injection is handled globally in `src/hooks.client.ts` via request interceptors.

## Svelte 5 Runes Usage

This project uses Svelte 5 runes exclusively—no legacy `$:` reactive statements.

```svelte
<script lang="ts">
	// Props with typing
	const { data }: PageProps = $props();

	// Reactive state
	let loading = $state(false);

	// Derived async values
	let accountsQuery = $derived.by(async () => {
		const query = await getApiAccount();
		return query.error ? [] : query.data!;
	});
</script>
```

## Route Structure & Conventions

- All authenticated routes live under `/app/*`
- Each route exports a `breadcrumbLabel` for automatic breadcrumb generation:
  ```svelte
  <script module lang="ts">
  	export const breadcrumbLabel = 'Konten';
  </script>
  ```
- Data loading in `+page.ts` files returns promises (streamed pattern):
  ```typescript
  export const load: PageLoad = () => ({
  	streamed: { dataQuery: fetchData() } // Promise, not awaited
  });
  ```

## Component Patterns

### Icon Usage

```svelte
<script>
	import MdiPencil from '~icons/mdi/pencil';
</script>

<MdiPencil class="text-lg" />
```

### DaisyUI Modal Pattern

```svelte
<dialog class="modal" bind:this={modalElement}>
	<div class="modal-box">...</div>
	<form method="dialog" class="modal-backdrop">
		<button>close</button>
	</form>
</dialog>
```

### Table with Loading State

```svelte
{#await dataQuery}
	<span class="loading loading-spinner loading-lg"></span>
{:then items}
	<table class="table-zebra table">...</table>
{:catch error}
	<ErrorAlert>{error}</ErrorAlert>
{/await}
```

## File Organization

- `src/lib/client/` - Auto-generated API client (DO NOT EDIT)
- `src/lib/components/` - Reusable UI components
- `src/lib/oidc.ts` - OIDC singleton wrapper
- `src/lib/formatter.ts` - Currency/number formatters (amounts in cents)
- `src/lib/aspsps.ts` - Bank data for Open Banking (static JSON, ~128k lines)
- `src/routes/app/` - Authenticated application routes

## Important Patterns

### Currency Handling

Amounts are stored as **cents** (integers). Use `formatEuroCents()` for display:

```typescript
import { formatEuroCents } from '$lib/formatter';
formatEuroCents(1234); // "12,34 €"
```

### Form Submissions

Use `onclick` handlers that `event.preventDefault()` and call API directly:

```svelte
<input type="submit" onclick={submitForm}>

async function submitForm(event: SubmitEvent) {
    event.preventDefault();
    const query = await postApiProduct({ body: formData });
    if (!query.error) goto('../');
}
```

## Backend Integration

- Dev proxy config in `vite.config.ts` routes `/api/*` to `localhost:8080`
- Auth routes `/realms/*`, `/resources/*` proxy to Keycloak at `localhost:9090`
- Regenerate API client after backend API changes: `npm run openapi-ts`
