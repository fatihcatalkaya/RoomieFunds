---
id: ROOMIE-3
title: Money inputs require raw cents instead of euro formatting
status: Done
assignee:
  - '@fatih'
created_date: '2026-08-08 14:25'
updated_date: '2026-08-08 15:07'
labels:
  - frontend
  - bug
dependencies: []
references:
  - docs/superpowers/plans/2026-08-08-money-input-euro-formatting.md
documentation:
  - 'https://pitis.github.io/svelte-number-format/'
priority: medium
ordinal: 3000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Monetary input fields let users enter amounts in raw cents (e.g. 100 for 1,00 €) instead of euro-decimal format (1,00). Verified in the live UI and source for four surfaces:

- Getränke → Neues Produkt (/app/products/create): 'Price' field binds product.price directly in cents; placeholder '100', no step.
- Getränke → Produkt bearbeiten (/app/products/edit/[id]): 'Price' field loads the raw cents value (shows '100' for the 1,00 € 'Bier' product).
- Daueraufträge → Neuer Dauerauftrag (/app/recurring-transactions/create): 'Betrag (ct)' field, step='1', binds formData.amount (cents).
- Daueraufträge → Dauerauftrag bearbeiten (/app/recurring-transactions/edit/[id]): 'Betrag' field binds cents directly, with no '(ct)' hint at all.

By contrast, the daily transaction entry (Konten → Konto → Transaktionen, TransactionInsert.svelte and the inline row edit in TransactionDisplayRow.svelte) is correctly formatted: type='number' step='0.01', converted as floatAmount * 100 on submit. These four forms should follow the same pattern and convert on input rather than expecting raw cents.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Product create Price field accepts '1,23' (euro format), not raw cents
- [x] #2 Product edit Price field displays and accepts '1,23'
- [x] #3 Recurring-transaction create and edit Betrag fields accept '1,23'
- [x] #4 A submitted value of '1,23' is stored/sent as 123 cents
- [x] #5 Existing tests covering these forms stay green
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
# ROOMIE-3 Money Inputs: Euro Formatting via svelte-number-format — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the four raw-cents money `<input type="number">` fields (product create/edit `Price`, recurring-transaction create/edit `Betrag`) with a shared `EuroInput` component built on `svelte-number-format`, forcing German locale (`de-DE`) so users type `1,23` (not `123`) and values are still stored as cents.

**Architecture:** One reusable Svelte 5 component, `src/lib/components/EuroInput.svelte`, wraps the library's `NumericFormat`. It owns the locale/currency/precision options (hard-coded German) and converts between the UI's euro float and the backend's cents integer, so every call site binds a plain cents value. The four route forms plus the two account-transaction inputs (`TransactionInsert`, `TransactionDisplayRow`) swap their `<input type="number">` for `<EuroInput bind:value=… />`.

**Tech Stack:** Svelte 5 (`^5.56.8`), `svelte-number-format@^2.0.0` (depends on `intl-number-input`), `Intl.NumberFormat` via the library. Frontend lives at `src/main/frontend/`.

## Global Constraints

- **Locale is forced to German.** Every money field MUST render/accept `de-DE` formatting (`1,23`), regardless of the user's browser locale. The library defaults `locale` to the browser locale — this plan hard-codes `locale="de-DE"` in `EuroInput` so that can't happen.
- **Currency:** `EUR`, `precision: 2` (2 fraction digits), `step: 0.01` (spinner moves 1 cent).
- **Storage stays in cents.** Backend DTOs keep integer cents (`amount`, `price`). `EuroInput` converts `cents / 100` ↔ `Math.round(euros * 100)`.
- **Svelte 5 runes only** (`$state`, `$effect`, `$props`, `$bindable`, `$derived`) — no stores, `svelte-number-format@2.x` is Svelte 5-native.
- There is **no frontend test framework** in this repo (no vitest, no `*.test.ts`). Definition-of-Done / acceptance criteria #5 ("existing tests stay green") is vacuous — no frontend tests exist to break. Verification is `npm run check`, `npm run build`, and a live browser smoke test.
- Match existing component conventions: PascalCase component names in `src/lib/components/`, daisyUI `input` class, Svelte 5 `interface Props` + `$props()`.

---

### Task 1: Add `svelte-number-format` dependency

**Files:**
- Modify: `src/main/frontend/package.json`

**Interfaces:**
- Consumes: nothing
- Produces: installable `svelte-number-format@^2.0.0` (Task 2 imports `NumericFormat` and `NumberFormatStyle` from it)

- [ ] **Step 1: Install**

Run in `src/main/frontend/`:

```bash
npm install svelte-number-format@^2.0.0
```

- [ ] **Step 2: Verify install**

```bash
npm list svelte-number-format
```

Expected: `svelte-number-format@2.x.x` listed (and `intl-number-input` as its dependency). Verify the exports exist:

```bash
node -e "import('svelte-number-format').then(m => console.log(Object.keys(m).sort().join('\n')))"
```

Expected output contains `NumericFormat` and `NumberFormatStyle`.

---

### Task 2: Create the `EuroInput` component

**Files:**
- Create: `src/main/frontend/src/lib/components/EuroInput.svelte`

**Interfaces:**
- Consumes: `NumericFormat`, `NumberFormatStyle` from `svelte-number-format`
- Produces: `<EuroInput bind:value={cents} />` — a two-way-bound field whose `value` is **integer cents**. Forwards `class`, `id`, `name`, `placeholder`, `required`, `disabled`, `form` onto the inner input. Tasks 3–6 consume this exact interface.

- [ ] **Step 1: Write the component**

Create `src/main/frontend/src/lib/components/EuroInput.svelte`:

```svelte
<script lang="ts">
	import { NumericFormat, NumberFormatStyle } from 'svelte-number-format';

	interface Props {
		// Value in cents (backend unit). Two-way bound.
		value?: number | null;
		class?: string;
		id?: string;
		name?: string;
		placeholder?: string;
		required?: boolean;
		disabled?: boolean;
		form?: string;
	}

	let { value = $bindable(null), class: className = '', ...rest }: Props = $props();

	// The input's own value is euros; mirror the cents value.
	let float: number | null = $state(value == null ? null : value / 100);

	// External value changes (edit-page load) refresh the displayed euros.
	$effect(() => {
		const euros = value == null ? null : value / 100;
		if (euros !== float) float = euros;
	});

	// Live input writes cents back to the bound value so submits are always current.
	function onValueInput(raw: number | null) {
		value = raw == null ? null : Math.round(raw * 100);
	}
</script>

<NumericFormat
	value={float}
	onInput={onValueInput}
	locale="de-DE"
	options={{
		formatStyle: NumberFormatStyle.Currency,
		currency: 'EUR',
		precision: 2,
		step: 0.01
	}}
	class={className}
	{...rest}
/>
```

Notes for the implementer:
- `onInput` (fires per keystroke) keeps `value` current even if the user submits without blurring — `NumericFormat`'s own `bind:value`/blur sync alone would be too late for form submission.
- The `$effect` guards against a write loop: values converge (writing equal cents is a no-op in Svelte 5), so no infinite effect churn.
- `locale="de-DE"` is hard-coded here — this is what "force German" means; do not move it to a prop/default.
- `value = $bindable(null)` lets `bind:value={x}` work and tolerates an initial `undefined` (field starts empty, matching current create-form behavior).

- [ ] **Step 2: Type-check**

```bash
npm run check
```

Run from `src/main/frontend/`. Expected: svelte-check succeeds (no new errors). If it reports the component is unused, that is fine — it becomes used in Tasks 3–6.

---

### Task 3: Use `EuroInput` in product create

**Files:**
- Modify: `src/main/frontend/src/routes/app/products/create/+page.svelte`

**Interfaces:**
- Consumes: `<EuroInput bind:value={price} />` from Task 2
- Produces: `price` (cents) sent to `postApiProduct`

- [ ] **Step 1: Import and swap the field**

In `src/main/frontend/src/routes/app/products/create/+page.svelte`:

1. In the `<script>` block, after the `postApiProduct` import, add:

```ts
import { EuroInput } from '$lib/components/EuroInput.svelte';
```

2. Change the `Price` input (currently `<input type="number" class="input w-3/4" placeholder="100" bind:value={price} />`) to:

```svelte
<label class="flex w-full items-center">
	<span class="w-1/4">Price</span>
	<EuroInput class="input w-3/4" placeholder="1,00" bind:value={price} />
</label>
```

(The `placeholder` is now an example amount, not a cents hint. Nothing else in the file changes; `commit` in `postProduct` still sends `price` in cents.)

- [ ] **Step 2: Type-check**

```bash
npm run check
```

Expected: no new errors.

---

### Task 4: Use `EuroInput` in product edit

**Files:**
- Modify: `src/main/frontend/src/routes/app/products/edit/[id]/+page.svelte`

**Interfaces:**
- Consumes: `<EuroInput bind:value={price} />` from Task 2
- Produces: `price` (cents) sent to `patchApiProductByProductId`

- [ ] **Step 1: Import and swap the field**

In `src/main/frontend/src/routes/app/products/edit/[id]/+page.svelte`:

1. In the `<script>` block, after the `MdiDelete` import, add:

```ts
import { EuroInput } from '$lib/components/EuroInput.svelte';
```

2. Change the `Price` input (currently `<input type="number" class="input w-3/4" bind:value={price} />`) to:

```svelte
<label class="flex w-full items-center">
	<span class="w-1/4">Price</span>
	<EuroInput class="input w-3/4" bind:value={price} />
</label>
```

`price` is `$state(product.price)` — already cents from the backend (`product.price`), and `EuroInput`'s initial-sync effect converts it to euros for display. On save, `updateProduct` sends `price` (cents) unchanged.

- [ ] **Step 2: Type-check**

```bash
npm run check
```

Expected: no new errors. If svelte-check complains about binding `number | undefined` from `product.price` to the component's nullable `value`, change the state line to `let price: number | null = $state(product.price ?? null);`.

---

### Task 5: Use `EuroInput` in recurring-transaction create

**Files:**
- Modify: `src/main/frontend/src/routes/app/recurring-transactions/create/+page.svelte`

**Interfaces:**
- Consumes: `<EuroInput bind:value={formData.amount} required />` from Task 2
- Produces: `formData.amount` (cents) in `postApiRecurringTransaction` body

- [ ] **Step 1: Import and swap the field**

In `src/main/frontend/src/routes/app/recurring-transactions/create/+page.svelte`:

1. In the `<script>` block, after the `getApiAccount` import, add:

```ts
import { EuroInput } from '$lib/components/EuroInput.svelte';
```

2. Change the `Betrag (ct)` input (currently `<input type="number" class="input w-3/4" bind:value={formData.amount} step="1" required />`) to:

```svelte
<label class="flex w-full items-center">
	<span class="w-1/4">Betrag</span>
	<EuroInput class="input w-3/4" bind:value={formData.amount} required />
</label>
```

Note the label text changes from `Betrag (ct)` to `Betrag` — the `(ct)` hint is obsolete once the field is euro-formatted. `required` forwards to the inner input (NumericFormat spreads rest props), preserving the current client-side validation. `formData.amount` is in `CreateRecurringTransactionDto` (cents) and is sent as-is.

- [ ] **Step 2: Type-check**

```bash
npm run check
```

Expected: no new errors.

---

### Task 6: Use `EuroInput` in recurring-transaction edit

**Files:**
- Modify: `src/main/frontend/src/routes/app/recurring-transactions/edit/[recurringTransactionId]/+page.svelte`

**Interfaces:**
- Consumes: `<EuroInput bind:value={formData.amount} />` from Task 2
- Produces: `formData.amount` (cents) in `patchApiRecurringTransactionByRecurringTransactionId` body

- [ ] **Step 1: Import and swap the field**

In `src/main/frontend/src/routes/app/recurring-transactions/edit/[recurringTransactionId]/+page.svelte`:

1. In the `<script>` block, after the `MdiDelete` import, add:

```ts
import { EuroInput } from '$lib/components/EuroInput.svelte';
```

2. Change the `Betrag` input (currently `<input type="number" class="input w-3/4" bind:value={formData.amount} />`) to:

```svelte
<label class="flex w-full items-center">
	<span class="w-1/4">Betrag</span>
	<EuroInput class="input w-3/4" bind:value={formData.amount} />
</label>
```

`formData.amount` is initialized from `recurringTransaction.amount` (cents) — `EuroInput`'s initial-sync effect shows it as euros. On save, `updateRecurringTransaction` sends `formData.amount` in cents.

- [ ] **Step 2: Type-check**

```bash
npm run check
```

Expected: no new errors. If svelte-check complains about binding `number | undefined` from `recurringTransaction.amount`, change the `formData` initializer to normalize: `amount: recurringTransaction.amount ?? null,` (and the `formData` type will accept it via `U`).

---

### Task 7: Use `EuroInput` in account transaction inputs

**Files:**
- Modify: `src/main/frontend/src/lib/components/TransactionInsert.svelte`
- Modify: `src/main/frontend/src/lib/components/TransactionDisplayRow.svelte`

**Interfaces:**
- Consumes: `<EuroInput bind:value={amount} />` from Task 2
- Produces: `amount` (cents, signed) sent to `postApiTransaction` / `patchApiTransactionByTransactionId`

These two inputs live on the account's transactions page (`/app/accounts/transactions/[accountId]/`): the **new-transaction** row (`TransactionInsert`) and the **inline row edit** (`TransactionDisplayRow`). Both currently bind a euro float (`floatAmount`) and convert on submit (`* 100`); both get migrated to `EuroInput` so every money input in the app flows through it.

Note on the sign: `dto.transaction.amount` is **signed** (negative = Abnahme/decrease). `EuroInput` displays `amount / 100` in de-DE (sign preserved); the effective sign on save is still governed by the direction radios (`direction` / `doChangeAmountSign`), which are unchanged — only the bound unit changes from euros to cents.

- [ ] **Step 1: Migrate the new-transaction input (`TransactionInsert.svelte`)**

1. In the `<script>` block, after the `MdiClose` import, add:

```ts
import { EuroInput } from '$lib/components/EuroInput.svelte';
```

2. Change the bound state from euro float to cents:

```ts
// before: let floatAmount: number = $state(0.0);
let amount: number | null = $state(null);
```

3. In `submitTransaction`, change the API body:

```ts
// before: amount: floatAmount * 100
amount: amount ?? 0
```

4. Replace the currency input block:

```svelte
<!-- before -->
<label class="input" lang="de">
	<input
		lang="de"
		class="min-w-20"
		form="transaction-new-form"
		bind:value={floatAmount}
		type="number"
		step="0.01"
	/>
	€
</label>

<!-- after -->
<EuroInput class="input min-w-20" form="transaction-new-form" bind:value={amount} />
```

(The `label`-wrapper, `lang="de"` and `€` suffix were only for native number parsing/display; `EuroInput` renders the `€` via currency formatting and handles `de-DE` itself.)

- [ ] **Step 2: Migrate the inline row-edit input (`TransactionDisplayRow.svelte`)**

1. In the `<script>` block, after the `MdiClose` import, add:

```ts
import { EuroInput } from '$lib/components/EuroInput.svelte';
```

2. Change the bound state from euro float to signed cents:

```ts
// before: let floatAmount: number = $state(dto.transaction?.amount! / 100.0);
let amount: number | null = $state(dto.transaction?.amount!);
```

3. In `allowEdit()`, change the reset:

```ts
// before: floatAmount = dto.transaction?.amount! / 100.0;
amount = dto.transaction?.amount!;
```

4. In `submitChange`, change the API body:

```ts
// before: amount: Math.round(floatAmount! * 100.0),
amount: amount ?? 0,
```

5. Replace the currency input block:

```svelte
<!-- before -->
<label class="input" lang="de">
	<input
		lang="de"
		form="transaction-new-form-{dto.transaction?.id}"
		bind:value={floatAmount}
		type="number"
		step="0.01"
	/>
	€
</label>

<!-- after -->
<EuroInput
	class="input min-w-20"
	form="transaction-new-form-{dto.transaction?.id}"
	bind:value={amount}
/>
```

- [ ] **Step 3: Type-check**

```bash
npm run check
```

Expected: no new errors. If svelte-check reports a type clash on `amount` (signed vs `number | null`), type it as `let amount: number | null = $state(dto.transaction?.amount ?? null);` for the initial value.

---

## Self-Review

**Spec coverage:**
- AC #1 (product create `1,23`) → Task 3.
- AC #2 (product edit displays & accepts `1,23`) → Task 4.
- AC #3 (recurring create & edit `1,23`) → Tasks 5, 6.
- AC #4 (`1,23` stored as `123` cents) → every task's submit path keeps cents; this now also covers the account-transactions inputs (`TransactionInsert` / `TransactionDisplayRow`), whose submit paths stay in cents (Task 7). End-to-end verification is the user's responsibility per working agreement.
- AC #5 (existing tests stay green) → vacuous: repo has no frontend test framework; noted in Global Constraints and handled by `npm run check`/`build`/lint instead.
- "Force locale to German" → `locale="de-DE"` hard-coded in `EuroInput` (Task 2), not exposed as an option.

**Placeholder scan:** No TBD/TODO; every task has concrete code and an immediate verification step. The earlier ticket's "Betrag (ct)"→"Betrag" label change is captured in Task 5.

**Type consistency:** `EuroInput` exposes `value?: number | null` (cents) with `$bindable`; all four routes bind either `price` or `formData.amount`, all cents-typed. The `float` (euros) state is internal to `EuroInput`. Tasks 3–6 note the one likely type-fix in each route (normalize `undefined`→`null`) so the plan is unambiguous.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Integration intent (fixed by this ticket): use NumericFormat for the four money fields. For each of product create/edit Price and recurring-transaction create/edit Betrag, bind a euro-float state (e.g. value = cents / 100) to <NumericFormat locale="de-DE" options={{ formatStyle: NumberFormatStyle.Currency, currency: 'EUR', precision: 2 }} />, and convert back to cents (Math.round(value * 100)) on submit — reusing the existing display formatter in src/lib/formatter.ts. This replaces the raw-cents step='1' inputs in the four referenced routes. Add 'svelte-number-format' to src/main/frontend dependencies.

Implementation plan written: docs/superpowers/plans/2026-08-08-money-input-euro-formatting.md (7 tasks). Locale forced to de-DE via a shared EuroInput component wrapping svelte-number-format's NumericFormat; cents <-> euro conversion centralized there.

Bugfix pass on the EuroInput integration: the component as first implemented was unusable. Two root causes, both found by tracing keystrokes in the live UI (Playwright, dev server on :5173).

1. Create forms crashed on load. EuroInput declared `value = $bindable(null)`, but the create forms bind an initially unset value (`let price: number | undefined = $state()`, `formData.amount`). Svelte throws `props_invalid_value` — 'Cannot do bind:value={undefined} when value has a fallback value' — so /app/products/create and /app/recurring-transactions/create never rendered past 'Warten auf Authentifizierung'. Fixed by dropping the fallback: `value = $bindable()`.

2. The field froze after the first keystroke. EuroInput wrote cents back to `value` on every keystroke, an $effect recomputed the euro float, and that float was passed down as NumericFormat's `value` prop. NumericFormat's init $effect *reads* `value`, so each change destroyed and re-created the underlying intl-number-input NumberInput and called setValue() — rewriting the text and parking the caret after the '€' suffix. Typing '12,34' produced: 1 -> '1,00 €' -> stuck at '1,00 €' for every further key (confirmed by an instrumented keystroke trace showing raw=1 repeating). Fixed by only re-assigning the euro state when `value` changes from the outside, tracked via a `syncedCents` mirror, so our own write-back never round-trips into the child.

Also switched the child to `bind:value` so NumericFormat reports the value it settled on at blur. Without that, re-applying a previously displayed amount (cancel + re-open the inline transaction row edit) would not have resynced the input. Dropped the no-op `step: 0.01` from options — it is not a NumberInputOptions key.

Verification (all through the live UI at 127.0.0.1:5173, backend on :8080; every write request was intercepted, so no records were created in the dev database):

- Keystroke trace, product edit Price: '' -> 1 -> 12 -> 12, -> 12,3 -> 12,34 -> blur '12,34 €', caret advancing 1,2,3,4,5. Before the fix the same trace was stuck at '1,00 €' caret=6 from the first key on.
- AC1 product create: typed '1,23', POST /api/product body {"name":"DIAG Testprodukt","price":123,"print":true}.
- AC2 product edit: loaded '1,00 €' for the 1,00 € Bier product, typed '1,23', PATCH /api/product/1 body {...,"price":123,...}.
- AC3 recurring create: typed '1,23', POST /api/recurring-transaction body {...,"amount":123}. Recurring edit (record mocked at 45678 cents): loaded '456,78 €', typed '1,23', PATCH body {...,"amount":123}.
- AC4 is covered by the four payloads above plus the two transaction inputs: TransactionInsert typed 1 -> 1, -> 1,2 -> 1,23, POST /api/transaction {...,"amount":123}; TransactionDisplayRow inline edit loaded '45,00 €', typed 9 -> 98 -> 98, -> 98,7 -> 98,76, and after cancel + re-open correctly showed '45,00 €' again, then '1,23' -> PATCH {...,"amount":123}.
- AC5: the repo has no frontend test framework (no vitest, no *.test.ts), so there are no tests to break. Instead: npm run build succeeds; npm run check reports 5 errors / 20 warnings, none in EuroInput.svelte or any of the six touched files (all in vite.config.ts, groups/edit, persons/edit, tally-count — pre-existing); npm run lint reports 138 pre-existing repo-wide errors, none in EuroInput.svelte; prettier --check on EuroInput.svelte is clean.

Side observation, NOT fixed and out of scope: /app/recurring-transactions/create cannot be submitted at all until both account selects are touched — they render with no option selected while being 'required'. Unrelated to the money field (the Betrag input itself validates fine).
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Money inputs on all six surfaces now take euro-decimal input (1,23) and still store integer cents, via a shared EuroInput component wrapping svelte-number-format's NumericFormat with locale forced to de-DE.

The initial integration was unusable and needed two fixes in EuroInput.svelte: (1) the $bindable(null) fallback made the create forms throw props_invalid_value and never render, since they bind an initially unset value — removed the fallback; (2) writing cents back to the bound value on every keystroke round-tripped into NumericFormat's value prop, whose init $effect re-creates the underlying intl-number-input NumberInput and calls setValue(), which rewrote the text and caret and froze the field after the first character — the euro state is now only re-assigned on external changes, tracked via a syncedCents mirror, and the child is bound with bind:value so it reports the value it settles on at blur.

Verified through the live dev UI with Playwright, with write requests intercepted so no dev-database records were created: keystroke traces build up 12,34 / 98,76 naturally with the caret advancing; product create+edit, recurring create+edit and both transaction inputs each loaded their stored cents formatted in German and sent amount/price 123 for typed input 1,23; the inline transaction row re-displays the original amount after cancel + re-open. npm run build passes; npm run check, npm run lint and prettier --check report no problems in EuroInput.svelte or any of the six touched files (remaining findings are pre-existing elsewhere in the repo). The repo has no frontend test framework, so AC5 had no tests to break.
<!-- SECTION:FINAL_SUMMARY:END -->
