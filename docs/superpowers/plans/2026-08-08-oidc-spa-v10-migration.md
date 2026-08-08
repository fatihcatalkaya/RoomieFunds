# oidc-spa v10 Migration with Backend-Provided OIDC Config — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Backlog ticket:** ROOMIE-1

**Goal:** Serve the public OIDC parameters (issuer URI, client ID) from the Quarkus backend via environment variables, and migrate the Svelte SPA from oidc-spa v6 (build-time Vite env vars) to oidc-spa v10 using its Framework Agnostic Adapter.

**Architecture:** A new unauthenticated JAX-RS resource `GET /api/config/oidc` returns `{issuerUri, clientId}` read from MicroProfile config properties that are backed by environment variables. On the frontend, `src/lib/oidc.ts` runs `oidcEarlyInit()` at module load, then `fetch`es that endpoint and feeds the result into `createOidc()`. One frontend bundle now works against any Keycloak.

**Tech Stack:** Quarkus 3 (JAX-RS / RESTEasy Reactive, MicroProfile Config, Lombok), Java 21, SvelteKit 2 + Svelte 5 with `adapter-static` (SPA, `ssr = false`, `prerender = true`), Vite 7, oidc-spa 10.x, Keycloak 26 from `docker-compose.yaml`.

## Global Constraints

- **Build the backend with JDK 21, never the machine default JDK 25.** Every `mvnw` invocation in this plan must be prefixed with `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`. On JDK 25 Lombok annotation processing is off by default and the build fails before your changes are even compiled. This is pre-existing — do not try to fix it here.
- **oidc-spa version:** `^10.2.11` (current `latest`). Do not use the `next` tag (`10.3.0-rc.x`).
- **Import path is `oidc-spa/core`, not `oidc-spa`.** In v10 the package root (`.`) re-exports only the entrypoint helpers. `createOidc`, `oidcEarlyInit` and the `Oidc` type all come from `oidc-spa/core`.
- **Do NOT use `oidc-spa/vite-plugin`.** It is the documented default for Vite projects, but for a non-React/non-Nuxt project it resolves the client entrypoint by parsing a root `index.html`, and throws `"oidc-spa: Could not locate index.html"` during `configResolved`. SvelteKit has no root `index.html` (it uses `src/app.html` and a generated JS client entry), so the plugin fails at startup in both `dev` and `build`. Use the documented **"Manual - Easy"** setup instead: call `oidcEarlyInit()` at the top of the same module that calls `createOidc()`. The oidc-spa docs note this option has a slightly weaker security posture than a split entrypoint; it is the only option SvelteKit's generated client entry leaves us.
- **Backend config property names:** `app.oidc.frontend.issuer-uri` and `app.oidc.frontend.client-id`, each defaulting from an env var with a docker-compose fallback, following the existing `${KC_SVC_CLIENT_SECRET:...}` style already used in `application.properties`.
- **No secrets in the new endpoint.** It is unauthenticated and public; it may only ever contain values that are already public in the browser (issuer URI, public client ID). Never add the backend's `quarkus.oidc.credentials.secret` or the service-account secret.
- **Do not edit Backlog markdown files directly.** Use the `backlog` CLI (`backlog task edit ROOMIE-1 --append-notes "..."`).
- **The frontend quality gates are already red before you start.** `npm run check` reports 722 pre-existing errors and `npm run lint` reports Prettier violations in 80 files. Leave them alone and scope your verification to the files you touch (Task 2, Steps 1/9/10). In particular **never run `npm run format`** — it would rewrite 80 unrelated files.
- **Do not regenerate `src/lib/client/`** (`npm run openapi-ts`). The new endpoint will show up in the OpenAPI document, but the bootstrap deliberately uses plain `fetch` (see Task 2 rationale) and a regeneration would produce a large unrelated diff.

## Testing approach — read this before Task 1

This repository has **no test infrastructure at all**: there is no `src/test/` directory, no `*.test.ts`, and no Playwright test project. Standing up JUnit/Vitest is not part of this ticket and would dwarf it.

So each task below still follows a red/green cycle, but the "test" is an **executable verification command run against the actual running system** (curl, `svelte-check`, playwright-cli). Every task starts by running the check and observing it fail, and ends by running the same check and observing it pass. **Paste the real output — never claim a check passed without having run it.**

## File Structure

**Created:**
- `src/main/java/de/flur4/roomiefunds/models/config/OidcConfigurationDto.java` — record carrying the two public OIDC values over the wire.
- `src/main/java/de/flur4/roomiefunds/infrastructure/web/OidcConfigurationController.java` — unauthenticated `GET /api/config/oidc`.

**Modified:**
- `src/main/resources/application.properties` — two new `app.oidc.frontend.*` properties.
- `src/main/frontend/package.json` — `oidc-spa` bumped from `^6.15.0` to `^10.2.11`.
- `src/main/frontend/vite.config.ts` — `optimizeDeps.exclude` for the oidc-spa subpath modules.
- `src/main/frontend/src/lib/oidc.ts` — rewritten: early init + runtime config fetch + `createOidc`; the `OidcWrapper` class is replaced by a `getOidc()` function.
- `src/main/frontend/src/hooks.client.ts` — v10 token API, and a single request interceptor instead of one per token rotation.
- `src/main/frontend/src/routes/+page.svelte` — `getOidc()` instead of `OidcWrapper`.
- `src/main/frontend/src/routes/app/+layout.svelte` — `getOidc()`, `Oidc` type from `oidc-spa/core`.
- `src/main/frontend/.env.example` — the two removed `VITE_OIDC_*` vars replaced by a pointer to the backend.
- `README.md` — document the new backend env vars.

---

## Task 1: Backend endpoint serving the public OIDC configuration

Delivers `GET /api/config/oidc` returning `{"issuerUri": "...", "clientId": "..."}` with no auth required, sourced from environment variables.

**Files:**
- Create: `src/main/java/de/flur4/roomiefunds/models/config/OidcConfigurationDto.java`
- Create: `src/main/java/de/flur4/roomiefunds/infrastructure/web/OidcConfigurationController.java`
- Modify: `src/main/resources/application.properties` (append a new section after the existing `# OIDC Configuration` block)
- Modify: `README.md` (local infrastructure section)

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: HTTP contract `GET /api/config/oidc` → `200 application/json` with exactly the keys `issuerUri` (string) and `clientId` (string). Task 2 depends on these exact key names.

---

- [ ] **Step 1: Start the local infrastructure**

The endpoint check needs a running backend, and the backend needs PostgreSQL (Flyway migrates at startup) plus Keycloak.

```bash
cd /home/fatih/IdeaProjects/RoomieFunds
docker compose up -d
docker compose ps
```

Expected: both `db` and `keycloak` are `running`; `keycloak` reaches `healthy` after ~30–60s. Re-run `docker compose ps` until it does.

- [ ] **Step 2: Start the backend in dev mode (leave it running for the whole task)**

Run this in a background shell — Quarkus dev mode hot-reloads, so you start it once and it picks up the new controller automatically.

```bash
cd /home/fatih/IdeaProjects/RoomieFunds
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./mvnw quarkus:dev
```

Wait until the log prints `Listening on: http://0.0.0.0:8080`. If it fails to start, check the Global Constraints — the JDK 21 prefix is not optional.

- [ ] **Step 3: Write the failing check — confirm the endpoint does not exist yet**

```bash
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/config/oidc
```

Expected: `404`. (Not `200`. If you get `200`, someone already did this task — stop and re-read the current state of the repo.)

- [ ] **Step 4: Create the DTO**

The project models wire types as plain records under `de.flur4.roomiefunds.models.<area>`, so this follows `LogEntryDto` / `CreateGroupDto`.

Create `src/main/java/de/flur4/roomiefunds/models/config/OidcConfigurationDto.java`:

```java
package de.flur4.roomiefunds.models.config;

/**
 * Public OIDC parameters the browser needs in order to talk to the identity provider.
 * Everything in here ends up in the SPA bundle at runtime, so it must never carry a secret.
 */
public record OidcConfigurationDto(String issuerUri, String clientId) {
}
```

- [ ] **Step 5: Create the controller**

Every other controller in `infrastructure/web` is annotated `@RolesAllowed({"roomiefunds-admin"})`. This one must not be: the SPA has to read it *before* it can authenticate. `@PermitAll` states that intent explicitly rather than relying on the absence of an annotation.

Create `src/main/java/de/flur4/roomiefunds/infrastructure/web/OidcConfigurationController.java`:

```java
package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.models.config.OidcConfigurationDto;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Hands the SPA the OIDC parameters it needs to bootstrap oidc-spa.
 *
 * Deliberately unauthenticated: the frontend calls this before it has a token. Only values that are
 * public by nature (the issuer URI and the public client id) belong here.
 */
@Path("/api/config/oidc")
@PermitAll
@JBossLog
public class OidcConfigurationController {

    @ConfigProperty(name = "app.oidc.frontend.issuer-uri")
    String issuerUri;

    @ConfigProperty(name = "app.oidc.frontend.client-id")
    String clientId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public OidcConfigurationDto getOidcConfiguration() {
        return new OidcConfigurationDto(issuerUri, clientId);
    }
}
```

- [ ] **Step 6: Add the configuration properties**

In `src/main/resources/application.properties`, directly below the existing `# OIDC Configuration` block (the one ending with `quarkus.oidc.credentials.secret=roomiefunds-dev-secret`), add:

```properties
# Public OIDC configuration handed to the frontend by OidcConfigurationController.
# Override per environment with the OIDC_FRONTEND_ISSUER_URI / OIDC_FRONTEND_CLIENT_ID env vars.
# The defaults target the Keycloak from docker-compose.yaml.
app.oidc.frontend.issuer-uri=${OIDC_FRONTEND_ISSUER_URI:http://localhost:9090/realms/roomiefunds}
app.oidc.frontend.client-id=${OIDC_FRONTEND_CLIENT_ID:roomiefunds-frontend}
```

Note this is the *frontend's* issuer, deliberately separate from `quarkus.oidc.auth-server-url` (the backend's). They are identical locally but diverge in deployments where the backend reaches Keycloak over an internal hostname the browser cannot resolve.

- [ ] **Step 7: Run the check — it should now pass**

Quarkus dev mode recompiles on the first request. Give it a second, then:

```bash
curl -s -i http://localhost:8080/api/config/oidc
```

Expected:
```
HTTP/1.1 200 OK
content-type: application/json;charset=UTF-8

{"issuerUri":"http://localhost:9090/realms/roomiefunds","clientId":"roomiefunds-frontend"}
```

Confirm the request carried **no** `Authorization` header — `curl` sent none, which is the point.

- [ ] **Step 8: Verify the env-var override actually works**

This is acceptance criterion #2 and is easy to get wrong (e.g. by hardcoding the value). Restart the backend with the env vars set:

Stop the dev-mode process, then:

```bash
cd /home/fatih/IdeaProjects/RoomieFunds
OIDC_FRONTEND_ISSUER_URI=https://auth.example.test/realms/other \
OIDC_FRONTEND_CLIENT_ID=some-other-client \
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./mvnw quarkus:dev
```

Once it is listening:

```bash
curl -s http://localhost:8080/api/config/oidc
```

Expected: `{"issuerUri":"https://auth.example.test/realms/other","clientId":"some-other-client"}`

Then stop it and restart plain `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./mvnw quarkus:dev` so the defaults are back for the following tasks.

- [ ] **Step 9: Verify the backend still compiles from clean**

Dev mode is forgiving; the real build is not.

```bash
cd /home/fatih/IdeaProjects/RoomieFunds
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./mvnw -o compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 10: Document the new environment variables in the README**

In `README.md`, in the "Local infrastructure" section, add a short subsection after the existing configuration table:

```markdown
### OIDC configuration for the frontend

The SPA no longer bakes its OIDC settings into the bundle. It reads them at startup from
`GET /api/config/oidc`, which the backend serves unauthenticated from these environment variables:

| Environment variable        | Default (docker-compose)                        | Meaning                          |
|-----------------------------|-------------------------------------------------|----------------------------------|
| `OIDC_FRONTEND_ISSUER_URI`  | `http://localhost:9090/realms/roomiefunds`      | Issuer the browser authenticates against |
| `OIDC_FRONTEND_CLIENT_ID`   | `roomiefunds-frontend`                          | Public OIDC client of the SPA    |

These are separate from `quarkus.oidc.auth-server-url`, which is how the *backend* reaches Keycloak —
in a deployment the backend may use an internal hostname the browser cannot resolve.
```

- [ ] **Step 11: Commit**

```bash
cd /home/fatih/IdeaProjects/RoomieFunds
git add src/main/java/de/flur4/roomiefunds/models/config/OidcConfigurationDto.java \
        src/main/java/de/flur4/roomiefunds/infrastructure/web/OidcConfigurationController.java \
        src/main/resources/application.properties \
        README.md
git commit -m "feat(auth): serve public OIDC configuration to the frontend

Adds an unauthenticated GET /api/config/oidc returning the issuer URI and
public client id, sourced from OIDC_FRONTEND_ISSUER_URI and
OIDC_FRONTEND_CLIENT_ID so a single frontend bundle works in any environment."
```

- [ ] **Step 12: Record progress on the ticket**

```bash
backlog task edit ROOMIE-1 --append-notes "Task 1 done: GET /api/config/oidc returns {issuerUri, clientId} unauthenticated; values come from OIDC_FRONTEND_ISSUER_URI / OIDC_FRONTEND_CLIENT_ID with docker-compose defaults. Verified with curl including an env-var override run."
```

---

## Task 2: Upgrade the frontend to oidc-spa v10 and bootstrap it from the backend

Delivers a frontend that type-checks and lints against oidc-spa v10 and gets its issuer/client id at runtime. All four files that touch oidc-spa change together — after the version bump the v6 API no longer exists, so splitting this would leave the intermediate state uncompilable.

**Files:**
- Modify: `src/main/frontend/package.json`
- Modify: `src/main/frontend/vite.config.ts`
- Modify: `src/main/frontend/src/lib/oidc.ts` (full rewrite)
- Modify: `src/main/frontend/src/hooks.client.ts`
- Modify: `src/main/frontend/src/routes/+page.svelte`
- Modify: `src/main/frontend/src/routes/app/+layout.svelte`
- Modify: `src/main/frontend/.env.example`

**Interfaces:**
- Consumes: `GET /api/config/oidc` → `{issuerUri: string, clientId: string}` from Task 1.
- Produces: `src/lib/oidc.ts` exports `getOidc(): Promise<Oidc>` — a single memoized instance. `OidcWrapper` is gone; nothing may import it after this task. `Oidc` is the type exported from `oidc-spa/core`, which is the union `Oidc.LoggedIn<...> | Oidc.NotLoggedIn`.

**v6 → v10 API changes you will apply (from the v10 `.d.ts`, verified):**

| v6 | v10 |
|---|---|
| `import { createOidc, type Oidc } from 'oidc-spa'` | `import { createOidc, oidcEarlyInit, type Oidc } from 'oidc-spa/core'` |
| `createOidc({ ..., homeUrl: window.location.origin })` | `homeUrl` removed; `BASE_URL` is supplied to `oidcEarlyInit()` |
| `oidc.getTokens_next()` | `oidc.getTokens()` |
| `Oidc.LoggedIn<Record<string, unknown>> \| Oidc.NotLoggedIn` | just `Oidc` (same union, default type param) |

`login({doesCurrentHrefRequiresAuth})`, `logout({redirectTo: 'specific url', url})`, `isUserLoggedIn` and `subscribeToTokensChange` are unchanged in v10 — do not "modernise" them.

---

- [ ] **Step 1: Record the baseline — and read this, because the baseline is not clean**

```bash
cd /home/fatih/IdeaProjects/RoomieFunds/src/main/frontend
npm run check
```

Expected (measured on this branch before any change): **`svelte-check found 722 errors and 0 warnings in 4 files`**, distributed as:

| File | Errors |
|---|---|
| `src/lib/aspsps.ts` | 718 |
| `src/routes/app/products/tally-count/+page.svelte` | 2 |
| `src/routes/app/persons/edit/[id]/+page.svelte` | 1 |
| `src/routes/app/groups/edit/[id]/+page.svelte` | 1 |

All of these are pre-existing and unrelated to auth. **Do not fix them** — that is a different ticket, and a 718-error file would swamp this diff.

None of the four files this task touches appear in that list, so your success criterion is precise: **the count must come back to 722, and none of the errors may be in a file you edited.**

`npm run lint` is likewise already failing at baseline: Prettier reports *"Code style issues found in 80 files"*. **Never run `npm run format`** during this task — it rewrites all 80 and buries the actual change. Step 10 formats only the files you touched.

- [ ] **Step 2: Install oidc-spa v10**

```bash
cd /home/fatih/IdeaProjects/RoomieFunds/src/main/frontend
npm install oidc-spa@^10.2.11
```

Then confirm the resolved version and that `package.json` now says `"oidc-spa": "^10.2.11"`:

```bash
npm ls oidc-spa
```

Expected: `oidc-spa@10.2.x`.

`zod` is listed as recommended by the oidc-spa docs for validating the decoded ID token. We do not read custom ID-token claims, so skip it — do not add the dependency.

- [ ] **Step 3: Run the check to see it fail**

```bash
npm run check
```

Expected: the error count climbs **above** the 722 baseline, with new errors in `src/lib/oidc.ts`, `src/hooks.client.ts` and `src/routes/app/+layout.svelte` — things like `Cannot find module 'oidc-spa'`, `Property 'getTokens_next' does not exist`, and `Object literal may only specify known properties, and 'homeUrl' does not exist`. That is the red state this task turns green.

- [ ] **Step 4: Exclude oidc-spa from Vite's dependency pre-bundling**

The oidc-spa Vite plugin (which we cannot use — see Global Constraints) does exactly this for non-React projects. Pre-bundling would rewrite the module graph oidc-spa's early init relies on, so replicate it by hand.

In `src/main/frontend/vite.config.ts`, add an `optimizeDeps` block next to the existing `esbuild` block:

```typescript
export default defineConfig({
	esbuild: {
		supported: {
			'top-level-await': true
		}
	},
	// The oidc-spa Vite plugin does this for us in other stacks, but it only supports projects with a
	// root index.html, which SvelteKit does not have. Pre-bundling oidc-spa breaks its early init.
	optimizeDeps: {
		exclude: ['oidc-spa', 'oidc-spa/core', 'oidc-spa/entrypoint']
	},
	plugins: [
```

Leave the rest of the file (plugins, the `/realms`, `/resources` and `/api` dev proxies) untouched — the `/api` proxy is what makes the config fetch work against `localhost:8080` in dev.

- [ ] **Step 5: Rewrite `src/lib/oidc.ts`**

Replace the entire contents of `src/main/frontend/src/lib/oidc.ts` with:

```typescript
import { createOidc, oidcEarlyInit, type Oidc } from 'oidc-spa/core';

// Must run before anything else inspects the URL, because oidc-spa picks the auth response out of it.
// The oidc-spa Vite plugin would normally do this from a generated client entrypoint, but it resolves
// that entrypoint from a root index.html which SvelteKit does not have. So we use the documented
// "Manual - Easy" setup: https://docs.oidc-spa.dev/v/v10/integration-guides/usage
oidcEarlyInit({ BASE_URL: import.meta.env.BASE_URL });

/** Mirrors OidcConfigurationDto served by the backend at GET /api/config/oidc. */
type OidcConfiguration = {
	issuerUri: string;
	clientId: string;
};

/**
 * The backend owns the OIDC parameters so one built bundle works in every environment.
 * Uses plain fetch rather than the generated OpenAPI client: this runs before the client's bearer
 * token interceptor is installed, and the endpoint needs no authentication anyway.
 */
async function fetchOidcConfiguration(): Promise<OidcConfiguration> {
	const response = await fetch('/api/config/oidc');

	if (!response.ok) {
		throw new Error(
			`Could not load the OIDC configuration from the backend: ${response.status} ${response.statusText}`
		);
	}

	return (await response.json()) as OidcConfiguration;
}

const prOidc: Promise<Oidc> = fetchOidcConfiguration().then(({ issuerUri, clientId }) =>
	createOidc({ issuerUri, clientId })
);

export async function getOidc(): Promise<Oidc> {
	return prOidc;
}
```

Two things to be aware of, both intentional:
- `prOidc` is created at module scope, so the config is fetched once per page load and every caller of `getOidc()` shares one oidc-spa instance. This replaces what the old `OidcWrapper` singleton did.
- `oidcEarlyInit()` runs synchronously while `createOidc()` is deferred behind the fetch. That is supported: early init captures the redirect auth response up front and `createOidc` consumes it afterwards. Task 3 verifies the round trip for real.

- [ ] **Step 6: Update `src/hooks.client.ts`**

Two changes: the v10 token API, and a real bug — the old code registered a *new* request interceptor on every token rotation, so the interceptor list grew unbounded for the life of the session. One interceptor reading a closed-over variable fixes it.

Replace the entire contents of `src/main/frontend/src/hooks.client.ts` with:

```typescript
import { client } from '$lib/client/client.gen';
import { getOidc } from '$lib/oidc';
import type { ClientInit } from '@sveltejs/kit';

export const init: ClientInit = async () => {
	const oidc = await getOidc();

	if (!oidc.isUserLoggedIn) {
		return;
	}

	// Keep the generated OpenAPI client's bearer token in sync with the tokens oidc-spa rotates in the
	// background. A single interceptor reads the latest value through the closure — registering a new
	// interceptor per rotation would leak one per refresh.
	let accessToken = (await oidc.getTokens()).accessToken;

	oidc.subscribeToTokensChange((tokens) => {
		accessToken = tokens.accessToken;
	});

	client.interceptors.request.use((request) => {
		request.headers.set('Authorization', `Bearer ${accessToken}`);
		return request;
	});
};
```

- [ ] **Step 7: Update `src/routes/+page.svelte`**

Replace the entire contents of `src/main/frontend/src/routes/+page.svelte` with:

```svelte
<script lang="ts">
	import { goto } from '$app/navigation';
	import { getOidc } from '$lib/oidc';
	import { onMount } from 'svelte';

	async function initializeOidc() {
		const oidc = await getOidc();
		if (!oidc.isUserLoggedIn) {
			oidc.login({
				doesCurrentHrefRequiresAuth: false
			});
		} else {
			goto('/app');
		}
	}

	onMount(() => {
		initializeOidc();
	});
</script>
```

- [ ] **Step 8: Update `src/routes/app/+layout.svelte`**

Only the `<script>` block changes — leave all the markup below it exactly as it is.

In `src/main/frontend/src/routes/app/+layout.svelte`, change these three lines:

```diff
-	import { OidcWrapper } from '$lib/oidc';
+	import { getOidc } from '$lib/oidc';
 	import { onMount } from 'svelte';
-	import type { Oidc } from 'oidc-spa';
+	import type { Oidc } from 'oidc-spa/core';
```

and replace the `oidcClient` declaration plus the `onMount` body:

```diff
-	let oidcClient: Oidc.LoggedIn<Record<string, unknown>> | Oidc.NotLoggedIn | null = $state(null);
+	let oidcClient: Oidc | null = $state(null);
 	onMount(async () => {
-		const oidcPromise = OidcWrapper.getInstance().getOidcClient();
-		oidcClient = await oidcPromise;
+		oidcClient = await getOidc();
 
 		if (!oidcClient.isUserLoggedIn) {
 			oidcClient.login({
 				doesCurrentHrefRequiresAuth: true
 			});
 		} else {
 			const result = await getApiPerson();
 			hasAccess = !result.error || result.response.status !== 403;
 		}
 	});
```

The `logout` function below stays byte-for-byte identical — `logout({ redirectTo: 'specific url', url: '/' })` is still the v10 signature.

- [ ] **Step 9: Run the check — it should be back to the baseline**

```bash
cd /home/fatih/IdeaProjects/RoomieFunds/src/main/frontend
npm run check 2>&1 | tail -3
```

Expected: `svelte-check found 722 errors and 0 warnings in 4 files` — exactly the Step 1 baseline.

Then prove none of those errors are yours:

```bash
npm run check 2>&1 | sed 's/\x1b\[[0-9;]*m//g' \
  | grep -E 'src/lib/oidc\.ts|src/hooks\.client\.ts|src/routes/\+page\.svelte|src/routes/app/\+layout\.svelte'
```

Expected: **no output**.

And prove no v6 API survived anywhere:

```bash
grep -rn "OidcWrapper\|getTokens_next\|from 'oidc-spa'" src/
```

Expected: **no output**. (Note the exact quoting: `from 'oidc-spa/core'` is correct and will not match this pattern.)

- [ ] **Step 10: Format and lint only the files you touched**

The repo has 80 files with pre-existing Prettier violations, so `npm run format` and `npm run lint` are both useless here — scope them instead.

```bash
cd /home/fatih/IdeaProjects/RoomieFunds/src/main/frontend
npx prettier --write vite.config.ts src/lib/oidc.ts src/hooks.client.ts \
  src/routes/+page.svelte "src/routes/app/+layout.svelte"
npx eslint vite.config.ts src/lib/oidc.ts src/hooks.client.ts \
  src/routes/+page.svelte "src/routes/app/+layout.svelte"
```

Expected: Prettier rewrites/confirms just those five files, and ESLint exits 0 with no output.

- [ ] **Step 11: Verify the production build works**

The SPA is built by `adapter-static` and served by `GatewayResource`; a dev-only success is not enough.

```bash
npm run build
```

Expected: `✓ built in ...`, then `Using @sveltejs/adapter-static` / `Wrote site to "dist"` / `✔ done`. Watch specifically for any oidc-spa resolution errors — those would mean the `optimizeDeps` change or the import paths are wrong.

(`dist/` and `.svelte-kit/` are gitignored, so building does not dirty the working tree.)

- [ ] **Step 12: Replace the build-time env vars in `.env.example`**

`VITE_OIDC_ISSUER_URI` and `VITE_OIDC_CLIENT_ID` no longer exist. Replace the entire contents of `src/main/frontend/.env.example` with:

```
# The OIDC issuer URI and client id are no longer build-time settings.
# The frontend reads them at startup from the backend: GET /api/config/oidc
#
# To point the app at a different Keycloak, set these on the *backend* instead:
#   OIDC_FRONTEND_ISSUER_URI=http://localhost:9090/realms/roomiefunds
#   OIDC_FRONTEND_CLIENT_ID=roomiefunds-frontend
#
# See README.md, section "OIDC configuration for the frontend".
```

Then confirm nothing still references the removed variables:

```bash
cd /home/fatih/IdeaProjects/RoomieFunds
grep -rn "VITE_OIDC" --exclude-dir=node_modules --exclude-dir=.svelte-kit --exclude-dir=dist .
```

Expected: no matches outside `backlog/` and `docs/superpowers/plans/` (the ticket and this plan legitimately mention them).

- [ ] **Step 13: Commit**

```bash
cd /home/fatih/IdeaProjects/RoomieFunds
git add src/main/frontend/package.json src/main/frontend/package-lock.json \
        src/main/frontend/vite.config.ts \
        src/main/frontend/src/lib/oidc.ts \
        src/main/frontend/src/hooks.client.ts \
        src/main/frontend/src/routes/+page.svelte \
        src/main/frontend/src/routes/app/+layout.svelte \
        src/main/frontend/.env.example
git commit -m "feat(frontend): migrate to oidc-spa v10 with runtime OIDC config

Upgrades oidc-spa 6.15 -> 10.2 and switches to the framework agnostic
adapter (oidcEarlyInit + createOidc from oidc-spa/core). Issuer URI and
client id now come from GET /api/config/oidc at startup instead of the
VITE_OIDC_* build-time variables.

Also collapses the per-rotation request interceptors in hooks.client.ts
into a single interceptor reading the current token."
```

- [ ] **Step 14: Record progress on the ticket**

```bash
backlog task edit ROOMIE-1 --append-notes "Task 2 done: oidc-spa on ^10.2.11, OidcWrapper replaced by getOidc(), config fetched from /api/config/oidc. Used the 'Manual - Easy' oidcEarlyInit setup because the oidc-spa Vite plugin needs a root index.html that SvelteKit does not have. svelte-check back at its 722-error pre-existing baseline with none in the touched files, scoped eslint clean, npm run build succeeds."
```

---

## Task 3: End-to-end verification with playwright-cli

Delivers evidence that login, authenticated API calls and logout work against the docker-compose Keycloak. This is acceptance criteria #6 and #7.

**Files:** none changed (unless a defect is found — then fix it here and note it).

**Interfaces:**
- Consumes: the running backend from Task 1 and the migrated frontend from Task 2.
- Produces: pasted playwright-cli output proving the flow.

Credentials, from `README.md` / the imported realm: user `user`, password `user`, member of group `roomiefunds-admin`.

---

- [ ] **Step 1: Make sure all three services are up**

```bash
cd /home/fatih/IdeaProjects/RoomieFunds
docker compose ps
curl -s http://localhost:8080/api/config/oidc
```

Expected: `db` and `keycloak` running, and the curl returns the JSON. If the backend is not running, start it again:
`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./mvnw quarkus:dev`

- [ ] **Step 2: Start the frontend dev server in the background**

```bash
cd /home/fatih/IdeaProjects/RoomieFunds/src/main/frontend
npm run dev
```

Expected: `Local: http://localhost:5173/`. The realm's `roomiefunds-frontend` client already allows `http://localhost:5173/*` as a redirect URI and web origin, so no Keycloak change is needed.

- [ ] **Step 3: Write the failing check — open the app and confirm we land on Keycloak**

```bash
playwright-cli open http://localhost:5173/
playwright-cli snapshot
```

Expected: the browser has been redirected to `http://localhost:9090/realms/roomiefunds/protocol/openid-connect/auth?...` and the snapshot shows the Keycloak sign-in form with username and password fields. This alone proves the runtime config fetch worked — without it, `createOidc` could not know the issuer.

If instead you are stuck on the app with no redirect, check the browser console before changing anything:

```bash
playwright-cli console
playwright-cli requests
```

- [ ] **Step 4: Log in**

Use the refs from the snapshot in Step 3 for the username and password inputs and the sign-in button.

```bash
playwright-cli fill <username-ref> "user"
playwright-cli fill <password-ref> "user"
playwright-cli click <signin-ref>
playwright-cli snapshot
```

Expected: back on `http://localhost:5173/app/`, snapshot shows the `RoomieFunds` header bar with the logout button — not the "Warten auf Authentifizierung" spinner and not the "Du hast keinen Zugriff auf diese Anwendung" panel.

- [ ] **Step 5: Confirm the API call was authenticated**

```bash
playwright-cli requests
```

Expected, in the request list: `GET /api/config/oidc` → 200, a token request to `/realms/roomiefunds/protocol/openid-connect/token` → 200, and `GET /api/person` → **200** (not 401/403). The 200 on `/api/person` is what proves `hooks.client.ts` attached the bearer token, since that endpoint is `@RolesAllowed({"roomiefunds-admin"})`.

Inspect the `/api/person` request in detail to see the `Authorization: Bearer ...` header:

```bash
playwright-cli request <index-of-api-person>
```

- [ ] **Step 6: Confirm there are no console errors**

```bash
playwright-cli console
```

Expected: no `error`-level entries. Keycloak or Vite may emit `warning`/`info` lines — those are fine. Any error mentioning `oidc-spa`, `BASE_URL`, or an unhandled rejection is a real failure: stop, fix it, and re-run from Step 3.

- [ ] **Step 7: Verify logout**

Take a snapshot to find the logout button (the red button with the logout icon in the header bar), then:

```bash
playwright-cli click <logout-ref>
playwright-cli snapshot
```

Expected: the session ends and the app returns to `/`, which immediately triggers `login({doesCurrentHrefRequiresAuth: false})` again — so you land back on the Keycloak sign-in form. That round trip is the confirmation that `logout({redirectTo: 'specific url', url: '/'})` still behaves the same as before the migration.

- [ ] **Step 8: Capture a screenshot as evidence and close the browser**

```bash
playwright-cli screenshot --filename=oidc-spa-v10-login-verified.png
playwright-cli close
```

- [ ] **Step 9: Record the verification on the ticket**

```bash
backlog task edit ROOMIE-1 --append-notes "Task 3 done: playwright-cli run against docker-compose Keycloak. localhost:5173 -> Keycloak login -> back to /app/ with GET /api/person 200 (bearer attached), no console errors, logout returns to the login page."
```

- [ ] **Step 10: Stop the dev servers**

Stop `npm run dev` and `./mvnw quarkus:dev`. Leave `docker compose` up or take it down as you prefer — `docker compose down` discards the Keycloak state, which is expected since the realm is re-imported on every start.

---

## Task 4: Finalize the ticket

**Files:** none.

- [ ] **Step 1: Read the finalization guide**

```bash
backlog instructions task-finalization
```

- [ ] **Step 2: Verify every acceptance criterion against the evidence gathered above, then check them off**

Do not tick a criterion you did not personally observe passing. Criteria #1/#2 map to Task 1 Steps 7–8, #3/#4/#5 to Task 2 Steps 9–12, #6/#7 to Task 3, #8 to Task 1 Step 10 and Task 2 Step 12.

- [ ] **Step 3: Write the final summary and move the ticket to Done, following whatever the finalization guide prescribes.**

---

## Risks and how they show up

- **`oidcEarlyInit` in a SvelteKit module may run later than oidc-spa expects.** SvelteKit owns its client entrypoint, so `oidc.ts` (imported by `hooks.client.ts`) is the earliest hook available. If the auth response is lost on the way back from Keycloak, Task 3 Step 4 fails by looping back to the login page instead of reaching `/app/`. Fallback: move the `oidcEarlyInit()` call into `src/hooks.client.ts` above the `getOidc()` import so it runs in SvelteKit's `init` hook before hydration.
- **Deferring `createOidc` behind a fetch.** Early init stores the redirect auth response, so an awaited `createOidc` should still find it. Same symptom and same detection point as above (Task 3 Step 4).
- **A failed config fetch leaves the SPA on the spinner forever.** `prOidc` rejects, `onMount`'s `await` throws, and the "Warten auf Authentifizierung" panel stays up. Acceptable for now — it only happens when the backend is down, in which case the app is unusable anyway. If it needs a proper error screen, that is follow-up work, not this ticket.

## Out of scope — do not do these here

- `username` in `src/routes/app/+layout.svelte` is declared as `$state` but never assigned, so the username chip in the header never renders. v10's `getDecodedIdToken().preferred_username` would fix it in one line, but it is a behaviour change no acceptance criterion asks for. Raise it as a follow-up ticket.
- Regenerating `src/lib/client/` from the OpenAPI document.
- Adding a test framework (JUnit for the backend, Vitest/Playwright test project for the frontend).
- Fixing the JDK 25 build failure.
