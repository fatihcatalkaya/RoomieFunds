---
id: ROOMIE-5
title: >-
  Replace Abnahme/Zunahme radios with a signed amount on the account
  transactions page
status: Done
assignee:
  - '@fatih'
created_date: '2026-08-08 16:12'
updated_date: '2026-08-08 17:00'
labels:
  - frontend
  - ux
dependencies: []
references:
  - docs/superpowers/plans/2026-08-08-signed-transaction-amounts.md
  - >-
    src/main/java/de/flur4/roomiefunds/infrastructure/renderer/accountstatement/TypstAccountStatementRenderer.java
documentation:
  - 'https://dm4t2.github.io/intl-number-input'
priority: medium
type: bug
ordinal: 5000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
On /app/accounts/transactions/[accountId] the direction of a booking is chosen with 'Abnahme' / 'Zunahme' radio buttons — in the new-transaction row (TransactionInsert.svelte) and in the inline row edit (TransactionDisplayRow.svelte). The radios only swap sourceAccountId and targetAccountId; the amount itself is always stored positive.

That does not work for a booking between an Aktiv and a Passiv account. TransactionService.getTransactionsForAccount sums the saldo like this:

    if (t.sourceAccountActive() != t.targetAccountActive()) saldo += t.amount();
    else if (t.sourceAccountId() == accountId)              saldo -= t.amount();
    else                                                    saldo += t.amount();

For a mixed Aktiv/Passiv pair the first branch ignores the source/target ordering entirely, so flipping the radios changes nothing about the saldo or the displayed amount. In the inline row edit the radios are additionally hard-disabled for exactly that case (disabled={sourceAccountActive != targetAccountActive}), so there the direction cannot be expressed at all. A negative amount is the only way to book a decrease against a mixed pair, and the backend already permits it: CreateTransactionDto.amount is a plain int with no @Positive constraint.

Replace both radio pairs with a signed Betrag field. The number the user types IS the effect on the currently opened account's saldo:

  -1,23  ->  the opened account goes down by 1,23 €
   1,23  ->  the opened account goes up by 1,23 €

The frontend derives what to send so the backend produces that effect, with no backend change:

  - mixed pair (source.active != target.active): send amount = typed value, sign included
  - same-type pair: send amount = |typed value|, and put the opened account in sourceAccountId when the typed value is negative, in targetAccountId when it is positive

The counter account then keeps following the existing bookkeeping rules — it moves in the opposite direction for a same-type pair, and in the same direction for a mixed Aktiv/Passiv pair, because a balance sheet grows and shrinks on both sides at once.

This convention already matches how the Betrag column renders today. TransactionDisplayRow displays amount * (doChangeAmountSign ? -1 : 1) and colours it red when that is negative, which is exactly the effect on the opened account. So the table needs no change: the new input is the inverse of the formula the table already uses.

Constraint found while scoping this: EuroInput cannot currently accept a negative value at all. intl-number-input 1.4.1 rejects the first '-' keypress — it only accepts a minus when the previously conformed value was already '-', so '--1' produces -1,00 € but '-1' produces 1,00 €, and appending '-' after digits does nothing. This was verified in the running dev UI against the baseline options and against autoSign:false and valueRange:{min:<0}; none of them change it. Programmatic negatives are fine (setValue(-12.34) renders '-12,34 €' and getValue().number is -12.34). EuroInput therefore has to handle the minus key itself.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Neither the new-transaction row nor the inline row edit shows Abnahme/Zunahme radio buttons
- [x] #2 Typing -1,23 in the new-transaction row books 1,23 € away from the opened account, for a same-type account pair and for a mixed Aktiv/Passiv pair alike
- [x] #3 Typing 1,23 in the new-transaction row books 1,23 € towards the opened account, for both pair kinds
- [x] #4 A single '-' keypress produces a negative amount in EuroInput, including on an empty field, and pressing '-' again returns it to positive
- [x] #5 Opening the inline row edit shows the signed effect the Betrag column already displays for that row, and saving it unchanged leaves the transaction untouched
- [x] #6 Editing a row to the opposite sign flips the booking direction, verified against both a same-type and a mixed Aktiv/Passiv pair
- [x] #7 EuroInput keeps rejecting negatives where they make no sense: product price and recurring-transaction Betrag are unchanged
- [x] #8 The printed account statement shows one correct sign per row for negative stored amounts (never '--1,23 €')
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
# Signed Transaction Amounts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Abnahme/Zunahme radio buttons on `/app/accounts/transactions/[accountId]` with a signed Betrag field, where the number the user types is exactly how the currently opened account's saldo moves.

**Architecture:** One pure module (`$lib/transactionAmount.ts`) owns the two directions of the conversion — the signed effect a stored transaction has on an account, and the `{amount, sourceAccountId, targetAccountId}` triple needed to produce a wanted effect. Both transaction components call it instead of carrying a `direction` state, and the existing Betrag column is refactored onto the same function so display and input can never drift apart. `EuroInput` gains an opt-in `allowNegative` mode that handles the minus key itself, because `intl-number-input` will not accept a typed `-`.

**Tech Stack:** Svelte 5 runes (`^5.56.8`), `svelte-number-format@2.0.0` over `intl-number-input@1.4.1`, SvelteKit + daisyUI, Quarkus 3 / Java 21 backend with a Typst statement renderer.

## Global Constraints

- **The typed number is the effect on the opened account's saldo.** `-1,23` means that account goes down by 1,23 €; `1,23` means it goes up. This is the single rule the whole page obeys.
- **No change to the saldo rules.** `TransactionService.getTransactionsForAccount` stays exactly as it is. The frontend adapts to it:
  - mixed pair (`source.active !== target.active`) → send `amount` **with its sign**, opened account as `sourceAccountId`
  - same-type pair → send `|amount|` and choose the ordering: opened account is the **source** when the typed value is negative, the **target** when it is positive
- **Storage stays in integer cents.** `CreateTransactionDto.amount` is an `int` with no `@Positive`; negative values are already accepted.
- **Locale stays German.** All money fields keep `locale="de-DE"`, `EUR`, `precision: 2`.
- **`allowNegative` is opt-in.** Only the two transaction amount fields set it. Product price and recurring-transaction Betrag must keep rejecting negatives (ROOMIE-5 AC #7).
- **Svelte 5 runes only** (`$state`, `$derived`, `$props`, `$bindable`, `$effect`) — no stores.
- **Git is human-only in this repo.** No task performs `git add`/`commit`/`push`; ask the human to commit at a checkpoint instead.
- **Frontend verification is browser-driven.** The repo has no frontend test framework (no vitest, no `*.test.ts`) and no `src/test` at all on the backend, so each task below ends with a concrete Playwright check against the running dev UI, following the method established in ROOMIE-3. See "Verification environment" below.

## Verification environment

The dev stack is expected to be running already:

- Frontend: `http://127.0.0.1:5173` (Vite; proxies `/api` → :8080, `/realms` → :9090)
- Login: user `user`, password `user`
- Backend, if it needs restarting: `JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./mvnw -Djooq quarkus:dev` from the repo root. The `-Djooq` flag is mandatory or the jOOQ generated sources are not on the source path; the machine default JDK 25 does not build this project.

Browser checks use `playwright-cli` (see `.claude/skills/playwright-cli`). **Intercept every write request with `page.route` and fulfill it with a stub**, as in ROOMIE-3, so verification does not write junk into the dev database. Where a check needs data that does not exist, mock the `GET` instead of creating records.

**Account data needed:** the checks below need the opened account plus one counter account of the *same* Aktiv/Passiv kind and one of the *opposite* kind. Confirm with:

```bash
playwright-cli --raw eval "fetch('/api/account').then(r=>r.json()).then(a=>JSON.stringify(a.map(x=>({id:x.id,name:x.name,active:x.active}))))"
```

If the mix is not there, mock `**/api/account` in the verification script rather than creating accounts.

## File Structure

| File | Responsibility |
|---|---|
| `src/main/frontend/src/lib/transactionAmount.ts` (new) | The only place that knows how a stored transaction maps to a signed per-account effect, and back. Pure, no Svelte, no I/O. |
| `src/main/frontend/src/lib/components/EuroInput.svelte` (modify) | Gains `allowNegative`; owns minus-key handling. |
| `src/main/frontend/src/lib/components/TransactionInsert.svelte` (modify) | New-transaction row: loses the radios and `direction`, takes the full `account`. |
| `src/main/frontend/src/lib/components/TransactionDisplayRow.svelte` (modify) | Existing row: loses the radios, `direction` and `doChangeAmountSign`; Betrag column and inline edit both go through the shared helper. |
| `src/main/frontend/src/routes/app/accounts/transactions/[accountId]/+page.svelte` (modify) | One line: passes `account` instead of `parentAccountId`. |
| `src/main/java/.../renderer/accountstatement/TypstAccountStatementRenderer.java` (modify) | Prints the sign from the saldo movement and the magnitude from `Math.abs`, so a negative stored amount cannot print `--1,23 €`. |

---

### Task 1: Teach `EuroInput` the minus key

**Files:**
- Modify: `src/main/frontend/src/lib/components/EuroInput.svelte`

**Interfaces:**
- Consumes: nothing
- Produces: `<EuroInput bind:value={cents} allowNegative />`. `value` stays **integer cents** and may now be negative. `allowNegative` defaults to `false`, so every existing call site is unaffected. Tasks 3 and 4 consume this exact prop name.

**Background the implementer needs:** `intl-number-input@1.4.1` refuses a typed `-`. Its `conformToMask` treats a minus as invalid whenever the field has no integer digits *and* the previously conformed value was not already `-`, so `-1` yields `1,00 €` while `--1` yields `-1,00 €`. `autoSign` and `valueRange` do not change this. Programmatic negatives work fine (`setValue(-12.34)` renders `-12,34 €`). So the component intercepts the key and sets the value itself, reusing the existing external-sync path that already pushes an outside change back into the field.

- [ ] **Step 1: Add the prop and the sign state**

In the `<script>` block, extend `Props` with `allowNegative` and destructure it:

```svelte
	interface Props {
		// Value in cents (backend unit). Two-way bound. May be negative when allowNegative is set.
		value?: number | null;
		// Opt-in: let the user type a leading minus. Off for fields where a
		// negative amount is meaningless, such as a product price.
		allowNegative?: boolean;
		class?: string;
		id?: string;
		name?: string;
		placeholder?: string;
		required?: boolean;
		disabled?: boolean;
		form?: string;
	}

	let {
		value = $bindable(),
		allowNegative = false,
		class: className = '',
		...rest
	}: Props = $props();
```

Then add, directly below the existing `syncedCents` declaration:

```svelte
	// Set when '-' is pressed on an empty field. intl-number-input cannot hold a
	// lone minus, so the sign waits here until the first digit arrives.
	let pendingNegative = false;
```

- [ ] **Step 2: Reset the pending sign when the value is replaced from outside**

Add one line to the existing `$effect`, so an outside write (an edit form loading, a cancelled edit reopening) never leaves a stale pending minus behind:

```svelte
	$effect(() => {
		if (value === syncedCents) return;
		syncedCents = value;
		pendingNegative = false;
		euros = toEuros(value);
	});
```

- [ ] **Step 3: Add the push-down helper and apply the pending sign**

Replace the existing `publish` function with these two functions. `pushDown` is the deliberate opposite of `publish`: it *does* refresh the displayed euros, which is what forces the field to re-render with the new sign.

```svelte
	// Writes cents outwards and refreshes what the field displays. Used only when
	// we change the sign ourselves — the plain publish() path deliberately leaves
	// the field alone so that typing is never interrupted.
	function pushDown(cents: number | null) {
		syncedCents = cents;
		value = cents;
		euros = toEuros(cents);
	}

	// Keeps the bound cents current on every keystroke, so submitting without
	// leaving the field still sends what the user sees.
	function publish(raw: number | null) {
		const cents = raw == null ? null : Math.round(raw * 100);
		if (pendingNegative && cents != null && cents !== 0) {
			pendingNegative = false;
			pushDown(-Math.abs(cents));
			return;
		}
		syncedCents = cents;
		value = cents;
	}
```

The `cents !== 0` guard keeps the pending minus alive while the user is still typing leading zeros, and avoids producing a negative zero.

- [ ] **Step 4: Handle the key**

Add below `publish`:

```svelte
	function onkeydown(event: KeyboardEvent) {
		if (!allowNegative) return;
		if (event.key !== '-' && event.key !== '+') return;
		event.preventDefault();

		const negate = event.key === '-';
		if (value == null) {
			// Nothing to flip yet — remember the sign for the first digit.
			pendingNegative = negate ? !pendingNegative : false;
			return;
		}
		pushDown(negate ? -Math.abs(value) : Math.abs(value));
	}
```

- [ ] **Step 5: Forward the handler to the inner input**

`NumericFormat` spreads every unknown prop onto its `<input>`, so passing `onkeydown` reaches the DOM. Put it **after** `{...rest}` so the component's own handler always wins:

```svelte
<NumericFormat
	bind:value={euros}
	onInput={publish}
	onChange={publish}
	locale="de-DE"
	options={{
		formatStyle: NumberFormatStyle.Currency,
		currency: 'EUR',
		precision: 2
	}}
	class={className}
	{...rest}
	{onkeydown}
/>
```

- [ ] **Step 6: Verify the minus key by hand in the browser**

`allowNegative` has no call site yet, so verify against a temporary one. Open a page that already has a `EuroInput` and drive it through a scratch instance is not possible without a call site, so instead temporarily add `allowNegative` to the product edit Price field:

In `src/main/frontend/src/routes/app/products/edit/[id]/+page.svelte`, change the Price field to `<EuroInput class="input w-3/4" bind:value={price} allowNegative />`.

Then run:

```bash
playwright-cli open http://127.0.0.1:5173/app/products/edit/1
# log in as user/user if prompted, then:
playwright-cli run-code "async page => {
  const el = page.getByRole('textbox', { name: 'Price' });
  const trial = async (keys) => {
    await el.click();
    await page.keyboard.press('ControlOrMeta+a');
    await page.keyboard.press('Delete');
    const seen = [];
    for (const k of keys) { await page.keyboard.type(k); await page.waitForTimeout(120); seen.push(await el.inputValue()); }
    await page.keyboard.press('Tab'); await page.waitForTimeout(250);
    return keys.join('') + ' -> ' + JSON.stringify(seen) + ' blur:' + await el.inputValue();
  };
  return [
    await trial(['-','1',',','2','3']),
    await trial(['1',',','2','3']),
    await trial(['1','-']),
    await trial(['1','-','-']),
    await trial(['-','-','1']),
  ];
}"
```

Expected:
- `-1,23` → ends `blur:"-1,23 €"` (the field stays empty on the `-`, then shows `-1` from the first digit)
- `1,23` → `blur:"1,23 €"`
- `1-` → `blur:"-1,00 €"` (flips an existing value)
- `1--` → `blur:"1,00 €"` (flips back)
- `--1` → `blur:"1,00 €"` (two minuses on an empty field cancel out)

- [ ] **Step 7: Revert the temporary call site**

Remove `allowNegative` from the product edit Price field again. It must stay off there (ROOMIE-5 AC #7).

- [ ] **Step 8: Confirm the default is still negative-free**

```bash
playwright-cli run-code "async page => {
  await page.reload(); await page.waitForTimeout(2500);
  const el = page.getByRole('textbox', { name: 'Price' });
  await el.click(); await page.keyboard.press('ControlOrMeta+a'); await page.keyboard.press('Delete');
  await page.keyboard.type('-1'); await page.waitForTimeout(200);
  await page.keyboard.press('Tab'); await page.waitForTimeout(250);
  return await el.inputValue();
}"
```

Expected: `"1,00 €"` — the minus is ignored again, because `allowNegative` is off.

- [ ] **Step 9: Type-check**

```bash
cd src/main/frontend && npm run check
```

Expected: no errors in `EuroInput.svelte`. The run reports 5 pre-existing errors elsewhere (`vite.config.ts`, `groups/edit`, `persons/edit`, `products/tally-count` ×2) — those are not yours.

---

### Task 2: The signed-amount conversion module

**Files:**
- Create: `src/main/frontend/src/lib/transactionAmount.ts`

**Interfaces:**
- Consumes: `Account` and `Transaction` types from `$lib/client`
- Produces:
  - `signedCentsFor(transaction: Transaction, openedAccountId: number): number`
  - `type Booking = { amount: number; sourceAccountId: number; targetAccountId: number }`
  - `bookingFor(signedCents: number, opened: AccountSide, counter: AccountSide): Booking`
  - `type AccountSide = { id?: number; active?: boolean }`

  Tasks 3 and 4 import all of these by these exact names.

**Background the implementer needs:** the backend decides how a transaction moves an account's saldo in `TransactionService.getTransactionsForAccount`:

```java
if (t.sourceAccountActive() != t.targetAccountActive()) saldo += t.amount();
else if (t.sourceAccountId() == accountId)              saldo -= t.amount();
else                                                    saldo += t.amount();
```

`signedCentsFor` is that rule expressed in TypeScript, and `bookingFor` is its inverse. They must stay exact mirrors of each other — every other file depends on the round trip holding.

- [ ] **Step 1: Write the module**

Create `src/main/frontend/src/lib/transactionAmount.ts`:

```ts
import type { Transaction } from '$lib/client';

/** Just the fields of an account that affect booking direction. */
export type AccountSide = {
	id?: number;
	active?: boolean;
};

export type Booking = {
	amount: number;
	sourceAccountId: number;
	targetAccountId: number;
};

/**
 * How many cents this transaction moves the given account's saldo by.
 * Negative means the account goes down.
 *
 * Mirrors TransactionService.getTransactionsForAccount: for a mixed
 * Aktiv/Passiv pair the stored amount applies as-is to both accounts, so its
 * own sign carries the direction. For a same-type pair the direction comes
 * from the ordering instead, and the source account is the one that goes down.
 */
export function signedCentsFor(transaction: Transaction, openedAccountId: number): number {
	const amount = transaction.amount ?? 0;

	if (transaction.sourceAccountActive !== transaction.targetAccountActive) {
		return amount;
	}

	return transaction.sourceAccountId === openedAccountId ? -amount : amount;
}

/**
 * The inverse of signedCentsFor: what to send so that the opened account's
 * saldo moves by exactly signedCents.
 *
 * The counter account then follows the existing bookkeeping rules on its own —
 * it moves the opposite way for a same-type pair, and the same way for a mixed
 * Aktiv/Passiv pair, because a balance sheet grows and shrinks on both sides.
 */
export function bookingFor(signedCents: number, opened: AccountSide, counter: AccountSide): Booking {
	if (opened.active !== counter.active) {
		return {
			amount: signedCents,
			sourceAccountId: opened.id!,
			targetAccountId: counter.id!
		};
	}

	return signedCents < 0
		? { amount: -signedCents, sourceAccountId: opened.id!, targetAccountId: counter.id! }
		: { amount: signedCents, sourceAccountId: counter.id!, targetAccountId: opened.id! };
}
```

- [ ] **Step 2: Check the round trip in a scratch script**

There is no test runner, so verify the two functions are true inverses with `node`. Write `/tmp/roundtrip.mjs`:

```js
const signedCentsFor = (t, openedAccountId) => {
	const amount = t.amount ?? 0;
	if (t.sourceAccountActive !== t.targetAccountActive) return amount;
	return t.sourceAccountId === openedAccountId ? -amount : amount;
};
const bookingFor = (signedCents, opened, counter) => {
	if (opened.active !== counter.active)
		return { amount: signedCents, sourceAccountId: opened.id, targetAccountId: counter.id };
	return signedCents < 0
		? { amount: -signedCents, sourceAccountId: opened.id, targetAccountId: counter.id }
		: { amount: signedCents, sourceAccountId: counter.id, targetAccountId: opened.id };
};

const opened = { id: 1, active: false };
const cases = [
	['same-type', { id: 2, active: false }],
	['mixed', { id: 3, active: true }]
];
let failures = 0;
for (const [label, counter] of cases) {
	for (const signed of [-4500, -123, -1, 1, 123, 4500]) {
		const b = bookingFor(signed, opened, counter);
		const back = signedCentsFor(
			{
				amount: b.amount,
				sourceAccountId: b.sourceAccountId,
				targetAccountId: b.targetAccountId,
				sourceAccountActive: b.sourceAccountId === opened.id ? opened.active : counter.active,
				targetAccountActive: b.targetAccountId === opened.id ? opened.active : counter.active
			},
			opened.id
		);
		const ok = back === signed;
		if (!ok) failures++;
		console.log(`${ok ? 'ok  ' : 'FAIL'} ${label} ${signed} -> ${JSON.stringify(b)} -> ${back}`);
	}
}
console.log(failures === 0 ? 'ALL ROUND TRIPS OK' : `${failures} FAILURES`);
```

Run it:

```bash
node /tmp/roundtrip.mjs
```

Expected: 12 `ok` lines and `ALL ROUND TRIPS OK`. If any line fails, the two functions have drifted — fix before continuing, because Tasks 3 and 4 both assume the round trip.

- [ ] **Step 3: Type-check**

```bash
cd src/main/frontend && npm run check
```

Expected: no errors in `transactionAmount.ts`. An "unused export" style warning is fine; Tasks 3 and 4 consume it.

---

### Task 3: Signed amount in the new-transaction row

**Files:**
- Modify: `src/main/frontend/src/lib/components/TransactionInsert.svelte`
- Modify: `src/main/frontend/src/routes/app/accounts/transactions/[accountId]/+page.svelte:191`

**Interfaces:**
- Consumes: `bookingFor` from Task 2, `allowNegative` from Task 1
- Produces: `<TransactionInsert {account} {refreshTransactions} />` — the prop changes from `parentAccountId: number` to `account: Account`, because the component now needs the opened account's `active` flag, and this matches how the sibling `TransactionDisplayRow` already takes its account.

- [ ] **Step 1: Swap the prop**

In the `<script>` block of `TransactionInsert.svelte`, add `type Account` to the existing `$lib/client` import, add the helper import, and change the props type:

```ts
	import {
		getApiAccount,
		getApiTransactionByTransactionIdReceipt,
		postApiTransaction,
		postApiTransactionByTransactionIdReceipt,
		type Account
	} from '$lib/client';
	import { bookingFor } from '$lib/transactionAmount';
```

```ts
	type TransactionInsertProps = {
		account: Account;
		refreshTransactions: () => void;
	};

	let { account, refreshTransactions }: TransactionInsertProps = $props();
```

- [ ] **Step 2: Drop the direction state**

Delete these two lines from the `<script>` block:

```ts
	type BookDirection = 'decrease' | 'increase';
```

```ts
	let direction: BookDirection = $state('decrease');
```

The `amount` state stays as it is (`let amount: number | null = $state(null);`) — it now simply carries a signed value.

- [ ] **Step 3: Build the booking on submit**

Replace `submitTransaction` with:

```ts
	async function submitTransaction(event: SubmitEvent) {
		event.preventDefault();

		const counterAccount = (await accountList).find((entry) => entry.id === bookAccountId);
		if (!counterAccount || amount == null || amount === 0) {
			return;
		}

		const query = await postApiTransaction({
			body: {
				valueDate: new Date(date).toISOString().substring(0, 10),
				description,
				...bookingFor(amount, account, counterAccount)
			}
		});

		if (query.error) {
			console.error(error);
		} else if (files && files.length > 0) {
			const receiptQuery = await postApiTransactionByTransactionIdReceipt({
				path: {
					transactionId: query.data?.id!
				},
				body: {
					receipt: files[0]
				}
			});

			if (receiptQuery.error) {
				console.error(error);
			}
		}

		refreshTransactions();
	}
```

Note the `console.log(files)` from the old version is dropped along the way.

- [ ] **Step 4: Update the account filter**

In the counter-account `<select>`, change the filter from `parentAccountId` to `account.id`:

```svelte
					{#each accountList as accountEntry}
						{#if accountEntry.id !== account.id}
							<option value={accountEntry.id}>{accountEntry.name}</option>
						{/if}
					{/each}
```

- [ ] **Step 5: Turn on negatives and remove the radios**

Change the amount cell to pass `allowNegative`:

```svelte
	<td>
		<EuroInput
			class="input min-w-20"
			form="transaction-new-form"
			bind:value={amount}
			allowNegative
		/>
	</td>
```

Then replace the whole radio cell — the `<td>` containing both `Abnahme` and `Zunahme` labels — with an empty cell. **Do not delete the `<td>`**: the header row has seven columns (Buchungsdatum, Beschreibung, Buchen Gegen, Beleg, Betrag, Saldo, Bearbeiten) and this cell sits under Saldo, so removing it would shift the submit button under the wrong header.

```svelte
	<td></td>
```

- [ ] **Step 6: Update the call site**

In `src/main/frontend/src/routes/app/accounts/transactions/[accountId]/+page.svelte` line 191:

```svelte
					<TransactionInsert {account} {refreshTransactions} />
```

- [ ] **Step 7: Type-check**

```bash
cd src/main/frontend && npm run check
```

Expected: no errors in `TransactionInsert.svelte` or the page.

- [ ] **Step 8: Verify both signs against both pair kinds**

This check mocks the account list so it always contains a same-type and a mixed counter account, and stubs the `POST` so nothing is written.

```bash
playwright-cli run-code "async page => {
  const captured = [];
  await page.route('**/api/account', r => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([
    { id: 1, name: 'Passiv:Opened', active: false },
    { id: 2, name: 'Passiv:SameType', active: false },
    { id: 3, name: 'Aktiv:Mixed', active: true }
  ])}));
  await page.route('**/api/account/1', r => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ id: 1, name: 'Passiv:Opened', active: false })}));
  await page.route('**/api/transaction/account/1', r => r.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
  await page.route('**/api/transaction', async r => {
    if (r.request().method() !== 'POST') return r.continue();
    captured.push(JSON.parse(r.request().postData()));
    await r.fulfill({ status: 200, contentType: 'application/json', body: '{\"id\":999}' });
  });
  await page.goto('http://127.0.0.1:5173/app/accounts/transactions/1');
  await page.waitForTimeout(2500);

  const amt = page.locator('input.min-w-20[form=\"transaction-new-form\"]');
  const book = async (counterId, keys) => {
    await page.locator('select[form=\"transaction-new-form\"]').selectOption(String(counterId));
    await amt.click();
    await page.keyboard.press('ControlOrMeta+a');
    await page.keyboard.press('Delete');
    for (const k of keys) { await page.keyboard.type(k); await page.waitForTimeout(110); }
    await page.getByRole('button', { name: 'Buchen' }).click();
    await page.waitForTimeout(700);
  };
  await book(2, ['-','1',',','2','3']);
  await book(2, ['1',',','2','3']);
  await book(3, ['-','1',',','2','3']);
  await book(3, ['1',',','2','3']);
  return captured.map(c => ({ amount: c.amount, src: c.sourceAccountId, tgt: c.targetAccountId }));
}"
```

Expected exactly:

```
[{"amount":123,"src":1,"tgt":2},    // same-type, -1,23 -> opened is source, goes down
 {"amount":123,"src":2,"tgt":1},    // same-type, +1,23 -> opened is target, goes up
 {"amount":-123,"src":1,"tgt":3},   // mixed, -1,23 -> sign carries the direction
 {"amount":123,"src":1,"tgt":3}]    // mixed, +1,23
```

Also confirm visually that the row no longer shows Abnahme/Zunahme and that the Buchen button is still under the Bearbeiten header:

```bash
playwright-cli find "Abnahme"
```

Expected: no matches.

---

### Task 4: Signed amount in the inline row edit

**Files:**
- Modify: `src/main/frontend/src/lib/components/TransactionDisplayRow.svelte`

**Interfaces:**
- Consumes: `signedCentsFor` and `bookingFor` from Task 2, `allowNegative` from Task 1
- Produces: no prop changes — `dto`, `account`, `refreshTransaction`, `tryDelete`, `tryDeleteReceipt` all stay as they are.

- [ ] **Step 1: Import the helpers**

Add to the `<script>` block, after the `EuroInput` import:

```ts
	import { bookingFor, signedCentsFor } from '$lib/transactionAmount';
```

- [ ] **Step 2: Replace the direction and sign state**

Delete the `type BookDirection = 'decrease' | 'increase';` line, the `direction` state, and the whole `doChangeAmountSign` derivation (including its stray `console.log`). Replace the `amount` and `bookAccountId` declarations with:

```ts
	// The signed effect on the opened account, in cents — the same number the
	// Betrag column shows, and the same number the user edits.
	// svelte-ignore state_referenced_locally
	let amount: number | null = $state(signedCentsFor(dto.transaction!, account.id!));

	// a $derived(...) would make sense here but we can't bind to that. Value is manually set in allowEdit()
	// svelte-ignore state_referenced_locally
	let bookAccountId: number | undefined = $state(
		dto.transaction?.sourceAccountId === account.id
			? dto.transaction?.targetAccountId
			: dto.transaction?.sourceAccountId
	);
```

- [ ] **Step 3: Update the edit reset**

Replace `allowEdit` with:

```ts
	async function allowEdit() {
		date = dto.transaction?.valueDate!;
		description = dto.transaction?.description!;
		amount = signedCentsFor(dto.transaction!, account.id!);
		bookAccountId =
			dto.transaction?.sourceAccountId === account.id
				? dto.transaction?.targetAccountId
				: dto.transaction?.sourceAccountId;

		editToggle = true;
	}
```

- [ ] **Step 4: Build the booking on save**

In `submitChange`, replace the `patchApiTransactionByTransactionId` call's body. The counter account has to be looked up for its `active` flag, exactly as in Task 3:

```ts
	async function submitChange(event: SubmitEvent) {
		event.preventDefault();

		const counterAccount = (await accountList).find((entry) => entry.id === bookAccountId);
		if (!counterAccount || amount == null || amount === 0) {
			return;
		}

		const query = await patchApiTransactionByTransactionId({
			path: {
				transactionId: dto.transaction?.id!
			},
			body: {
				valueDate: new Date(date!).toISOString().substring(0, 10),
				description: description!,
				...bookingFor(amount, account, counterAccount)
			}
		});
```

Leave the rest of `submitChange` (receipt upload, error handling, `refreshTransaction()`) untouched.

- [ ] **Step 5: Put the Betrag column on the same helper**

Replace the four-branch `{#if}` block that renders the amount cell with a single cell. Add the `{@const}` immediately after `{#if !editToggle}` (Svelte only allows `{@const}` as the direct child of a block):

```svelte
{#if !editToggle}
	{@const signedAmount = signedCentsFor(dto.transaction!, account.id!)}
	<tr>
```

and then, in place of the old four branches:

```svelte
		<td class="text-right {signedAmount < 0 ? 'font-semibold text-red-500' : ''}">
			{formatEuroCents(signedAmount)}
		</td>
```

This is the same output the old branches produced — they rendered `amount * (doChangeAmountSign ? -1 : 1)` and coloured it red when negative — but now display and input read from one function.

- [ ] **Step 6: Turn on negatives and remove the radios**

Change the edit-mode amount cell:

```svelte
		<td>
			<EuroInput
				class="input min-w-20"
				form="transaction-new-form-{dto.transaction?.id}"
				bind:value={amount}
				allowNegative
			/>
		</td>
```

Replace the radio `<td>` (both `Abnahme` and `Zunahme` labels, including the `disabled={...sourceAccountActive != ...targetAccountActive}` attributes) with an empty cell, for the same column-alignment reason as Task 3:

```svelte
		<td></td>
```

- [ ] **Step 7: Type-check**

```bash
cd src/main/frontend && npm run check
```

Expected: no errors in `TransactionDisplayRow.svelte`. The five pre-existing errors elsewhere remain.

- [ ] **Step 8: Verify load, round trip and sign flip**

```bash
playwright-cli run-code "async page => {
  const captured = [];
  const rows = [
    { transaction: { id: 42, sourceAccountId: 1, sourceAccountName: 'Passiv:Opened', sourceAccountActive: false,
                     targetAccountId: 2, targetAccountName: 'Passiv:SameType', targetAccountActive: false,
                     amount: 4500, valueDate: '2026-08-01', description: 'SameType Row', hasReceipt: false },
      saldo: -4500, sourceAccountNameParts: ['Passiv','Opened'], targetAccountNameParts: ['Passiv','SameType'] },
    { transaction: { id: 43, sourceAccountId: 1, sourceAccountName: 'Passiv:Opened', sourceAccountActive: false,
                     targetAccountId: 3, targetAccountName: 'Aktiv:Mixed', targetAccountActive: true,
                     amount: -2500, valueDate: '2026-08-02', description: 'Mixed Row', hasReceipt: false },
      saldo: -7000, sourceAccountNameParts: ['Passiv','Opened'], targetAccountNameParts: ['Aktiv','Mixed'] }
  ];
  await page.route('**/api/account', r => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([
    { id: 1, name: 'Passiv:Opened', active: false },
    { id: 2, name: 'Passiv:SameType', active: false },
    { id: 3, name: 'Aktiv:Mixed', active: true }
  ])}));
  await page.route('**/api/account/1', r => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ id: 1, name: 'Passiv:Opened', active: false })}));
  await page.route('**/api/transaction/account/1', r => r.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(rows) }));
  for (const id of [42, 43]) {
    await page.route('**/api/transaction/' + id, async r => {
      if (r.request().method() !== 'PATCH') return r.continue();
      captured.push(JSON.parse(r.request().postData()));
      await r.fulfill({ status: 200, contentType: 'application/json', body: '{}' });
    });
  }
  await page.goto('http://127.0.0.1:5173/app/accounts/transactions/1');
  await page.waitForTimeout(2500);

  const out = { columns: await page.locator('tbody tr td:nth-child(5)').allInnerTexts() };
  const editRow = async (id, keys) => {
    await page.locator('button[title=\"Buchung ' + id + ' Bearbeiten\"]').click();
    await page.waitForTimeout(400);
    const el = page.locator('input.min-w-20[form=\"transaction-new-form-' + id + '\"]');
    const loaded = await el.inputValue();
    if (keys) {
      await el.click();
      await page.keyboard.press('ControlOrMeta+a');
      await page.keyboard.press('Delete');
      for (const k of keys) { await page.keyboard.type(k); await page.waitForTimeout(110); }
    }
    await page.getByRole('button', { name: 'Speichern' }).click();
    await page.waitForTimeout(900);
    return loaded;
  };
  out.loaded42 = await editRow(42, null);
  out.loaded43 = await editRow(43, null);
  out.flipped42 = await editRow(42, ['4','5',',','0','0']);
  out.flipped43 = await editRow(43, ['2','5',',','0','0']);
  out.sent = captured.map(c => ({ amount: c.amount, src: c.sourceAccountId, tgt: c.targetAccountId }));
  return out;
}"
```

Expected:
- `columns` → `["-45,00 €", "-25,00 €"]` — both rows read as a decrease of the opened account
- `loaded42` → `"-45,00 €"`, `loaded43` → `"-25,00 €"` — the edit field shows what the column shows
- `sent[0]` → `{"amount":4500,"src":1,"tgt":2}` and `sent[1]` → `{"amount":-2500,"src":1,"tgt":3}` — saving unchanged reproduces the original rows exactly (ROOMIE-5 AC #5)
- `sent[2]` → `{"amount":4500,"src":2,"tgt":1}` — same-type row flipped to `+45,00` swaps the ordering
- `sent[3]` → `{"amount":2500,"src":1,"tgt":3}` — mixed row flipped to `+25,00` flips the stored sign

Then confirm the radios are gone from the edit row too:

```bash
playwright-cli find "Zunahme"
```

Expected: no matches.

---

### Task 5: Fix the sign in the printed account statement

**Files:**
- Modify: `src/main/java/de/flur4/roomiefunds/infrastructure/renderer/accountstatement/TypstAccountStatementRenderer.java:51-59`

**Interfaces:**
- Consumes: nothing from earlier tasks
- Produces: nothing consumed later. Independent of Tasks 1–4 and safe to do in either order, but it must ship in the same change, because Tasks 3 and 4 are what make negative amounts reachable.

**Background the implementer needs:** the renderer prefixes a sign and then prints `formatCurrency(tx.amount())` — the raw amount. That was safe only while every stored amount was positive. Once a mixed-pair booking can store `-123`, a decrease against a passive target prints `'-'` **and** a negative-formatted amount, giving `--1,23 €`. The existing rule also prints no sign at all for a decrease against an active target, which is wrong today already.

- [ ] **Step 1: Replace the sign block**

Replace this:

```java
            if(saldo < previousSaldo) {
                if(!tx.targetAccountActive()) {
                    sb.append('-');
                }
            } else {
                sb.append('+');
            }

            String amount = sb.append(formatCurrency(tx.amount())).toString();
```

with this:

```java
            // The sign shown is the direction this account's saldo moved, so the
            // amount itself is printed without one — a stored amount may be
            // negative for a booking between an Aktiv and a Passiv account.
            sb.append(saldo < previousSaldo ? '-' : '+');

            String amount = sb.append(formatCurrency(Math.abs(tx.amount()))).toString();
```

- [ ] **Step 2: Compile**

```bash
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./mvnw -Djooq -q compile
```

Expected: success. The machine's default JDK 25 does not build this project, and without `-Djooq` compilation fails with `Package de.flur4.roomiefunds.infrastructure.jooq ist nicht vorhanden`. The first run with `-Djooq` starts a Testcontainers PostgreSQL and regenerates the jOOQ classes, which takes 1–2 minutes.

- [ ] **Step 3: Check the rendered signs**

Restart the backend if it is not running in dev mode:

```bash
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./mvnw -Djooq quarkus:dev
```

Then download a statement for an account that has at least one increase and one decrease, and read the Betrag column:

```bash
playwright-cli run-code "async page => {
  await page.goto('http://127.0.0.1:5173/app/accounts/transactions/1');
  await page.waitForTimeout(2500);
  const [ , res ] = await Promise.all([
    page.getByRole('button').first().click(),
    page.waitForResponse(r => r.url().includes('/statement'))
  ]);
  return { status: res.status(), bytes: (await res.body()).length };
}"
```

Expected: status 200 and a non-empty body. Open the PDF and confirm every Betrag reads `+1,23 €` or `-1,23 €` — never `--1,23 €`, and never a bare amount with no sign.

If the account has no negative-amount transaction yet, create one through the UI first (this one is a real write; delete it afterwards with the row's Löschen button).

---

## Self-Review

**Spec coverage** — each ROOMIE-5 acceptance criterion maps to a task:

| AC | Task |
|---|---|
| #1 no radios in either row | Task 3 Step 5, Task 4 Step 6 (verified by the `find "Abnahme"` / `find "Zunahme"` checks) |
| #2 `-1,23` books away from the opened account, both pair kinds | Task 3 Step 8, first and third expected payloads |
| #3 `1,23` books towards it, both pair kinds | Task 3 Step 8, second and fourth expected payloads |
| #4 single `-` works, including on an empty field, and toggles back | Task 1 Steps 4 and 6 |
| #5 edit row loads the displayed signed effect; saving unchanged is a no-op | Task 4 Step 8, `loaded42`/`loaded43` and `sent[0]`/`sent[1]` |
| #6 flipping the sign flips the direction, both pair kinds | Task 4 Step 8, `sent[2]`/`sent[3]` |
| #7 product price and recurring Betrag still reject negatives | Task 1 Steps 7 and 8 (`allowNegative` defaults to `false` and the temporary call site is reverted) |

The statement renderer (Task 5) is not an AC on the ticket — it was added by decision during planning because Tasks 3 and 4 are what make negative amounts reachable. Add it as AC #8 if you want it tracked on the ticket.

**Placeholder scan:** none. Every step carries the actual code or the actual command plus its expected output.

**Type consistency:** `signedCentsFor(transaction, openedAccountId)` and `bookingFor(signedCents, opened, counter)` are used with those exact names and argument orders in Tasks 3 and 4. `allowNegative` is the prop name in Task 1 and at both call sites. `Booking` spreads into the request body as `amount`, `sourceAccountId`, `targetAccountId`, which match `CreateTransactionDto` and `UpdateTransactionDto`.

**Open question for the human, not a blocker:** this plan verifies a pure function (`transactionAmount.ts`) with a throwaway `node` script because the repo has no test runner. Adding `vitest` for that one module would be a small, contained improvement, but it is scope the ticket does not cover — say so if you want it and it becomes Task 0.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented on branch fix/roomie-5-signed-transaction-amounts (worktree off dev).

Deviation from the plan, found during verification: Task 1's design was wrong and had to be reworked. The plan applied the pending minus by calling pushDown() on the first digit, which changes the euros prop — that makes NumericFormat re-create its input, reformat the text to '-1,00 €' and park the caret past the '€', so every following keystroke was swallowed. That is exactly the freeze ROOMIE-3 fixed, reintroduced. Observed trace: '-1,23' produced '-1,00 €'.

Reworked sign handling in EuroInput:
- '-' with digits already present flips the sign in the input's OWN text and re-dispatches an input event. intl-number-input only rejects a minus when there are no integer digits, so this is accepted, it does not re-create the input, and focus/caret survive. Going through the value prop instead does not refresh a field whose euro value happens to be unchanged (euros goes stale during typing), which is why the first attempt at this path silently did nothing.
- '-' on an empty field still sets pendingNegative, but publish() now applies it by writing through that same captured input element, so the minus appears from the first digit rather than only at blur.
- Kept an onfocusout fallback that refreshes the field if the element write was not possible. focusout, not NumericFormat's onChange: onChange does not fire reliably on blur (observed: it fired once across a seven-case run), and focusout is dispatched after blur, so it is not overwritten by NumericFormat's own blur handler.
- pendingNegative is cleared on the text-flip path, otherwise a minus typed first and then again re-negated the result instead of toggling it back.

Everything else was implemented as planned. Lint on the touched files improves from 40 errors to 25, because the signedCentsFor refactor removes many 'dto.transaction?.x!' non-null assertions.

Verification, all against the dev stack running from this worktree (backend ./mvnw -Pjooq-local quarkus:dev, frontend npm run dev). Write requests were stubbed with page.route except where noted, so no dev-database records were created.

AC1 radios: 0 radio inputs named book-dir on the page, in the new-transaction row and in the inline edit row.
AC2/AC3 new-transaction payloads, mocked account list with a same-type (Passiv) and a mixed (Aktiv) counter:
  same-type -1,23 -> {amount:123, src:1, tgt:2}
  same-type  1,23 -> {amount:123, src:2, tgt:1}
  mixed     -1,23 -> {amount:-123, src:1, tgt:3}
  mixed      1,23 -> {amount:123, src:1, tgt:3}
AC4 sign entry, seven keystroke sequences, showing the field after each key:
  1,23    -> 1 1, 1,2 1,23            blur 1,23 €
  -1,23   -> -1 -1, -1,2 -1,23        blur -1,23 €
  1,23-   -> ... 1,23 -1,23           blur -1,23 €
  -1,23-  -> ... -1,23 1,23           blur 1,23 €
  --1,23  -> 1 1, 1,2 1,23            blur 1,23 €
  1,23--  -> ... -1,23 1,23           blur 1,23 €
  -45     -> -4 -45                   blur -45,00 €
AC5 inline edit, rows mocked as amount 4500 same-type and amount -2500 mixed: Betrag column shows -45,00 € and -25,00 €, the edit field loads the same, and saving unchanged sends {4500,src1,tgt2} and {-2500,src1,tgt3} — byte-identical to the originals.
AC6 sign flip: same-type row edited to +45,00 sends {4500,src2,tgt1}; mixed row edited to +25,00 sends {2500,src1,tgt3}.
AC7 negatives still rejected elsewhere: typing '-1,23' into product edit Price, product create Price and recurring-transaction Betrag all yield '1,23 €'.
AC8 statement PDF, verified before/after against a real row. The dev database has no Aktiv account, so the negative amount was produced by rewriting the app's own authenticated POST body to amount -223 (a negative amount breaks the renderer for any account pair, not only mixed ones). Rendered with pdftotext:
  old renderer: 08.08.2026  AC8 PROBE  Peter Lustig M002  --2,23 €  -4,46 €
  new renderer: 08.08.2026  AC8 PROBE  Peter Lustig M002   -2,23 €  -4,46 €
The probe transaction was deleted afterwards; the account is back to its single original row and a saldo of -2,23 €.

Gates: npm run build not run; npm run check reports the same 5 pre-existing errors as on dev (vite.config.ts, groups/edit, persons/edit, tally-count x2), none in the touched files; prettier --check clean on all five frontend files; Maven compile of the backend change succeeded under JDK 21 with -Pjooq-local.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
The Abnahme/Zunahme radios are gone from both the new-transaction row and the inline row edit on /app/accounts/transactions/[accountId]. The Betrag field is now signed: the number typed is exactly how the opened account's saldo moves, so '-1,23' books 1,23 € away from it and '1,23' books 1,23 € towards it. A new pure module, src/main/frontend/src/lib/transactionAmount.ts, holds both directions of the conversion (signedCentsFor / bookingFor) as exact inverses of the backend saldo rule, and the Betrag column was refactored onto the same function so display and input cannot drift. No backend saldo change was needed: for a mixed Aktiv/Passiv pair the stored amount carries the sign, for a same-type pair the source/target ordering does.

This also fixes the radios' real defect: for an Aktiv/Passiv pair the saldo ignores source/target ordering, which is all the radios changed, so they were inert there and hard-disabled in the edit row.

EuroInput gained an opt-in allowNegative mode that handles the minus key itself, because intl-number-input rejects a typed '-'. The planned approach for this was wrong and was reworked during verification — see the implementation notes. TypstAccountStatementRenderer now derives the printed sign from the saldo movement and formats the absolute amount, so a negative stored amount can no longer print '--2,23 €'.

Verified end-to-end in the browser against the dev stack: all four booking payloads for both account-pair kinds, seven sign-entry keystroke sequences, the inline edit round trip (saving unchanged reproduces the original row exactly) and the sign flip for both pair kinds, negatives still rejected on product price and recurring Betrag, and a real before/after of the statement PDF ('--2,23 €' before, '-2,23 €' after) with the probe row deleted afterwards. npm run check shows no new errors and lint on the touched files drops from 40 to 25.
<!-- SECTION:FINAL_SUMMARY:END -->
