---
id: ROOMIE-4
title: >-
  Transaction date display uses ISO format and new-transaction date default is
  invalid
status: Done
assignee:
  - '@fatih'
created_date: '2026-08-08 16:11'
updated_date: '2026-08-08 16:25'
labels:
  - frontend
  - bug
dependencies: []
priority: medium
type: bug
ordinal: 4000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Dates in the frontend must always be displayed in German locale format (dd.MM.yyyy), e.g. 8 August 2026 as "08.08.2026". On the account transactions page (/app/accounts/transactions/[accountId]/, e.g. /app/accounts/transactions/1/) the "Buchungsdatum" column instead shows the raw ISO value coming from the API, e.g. "2026-08-08".

This ticket covers two date defects on that same page.

## Defect 1 — ISO date rendered instead of German format

`src/lib/components/TransactionDisplayRow.svelte:146` renders `{dto.transaction?.valueDate}` directly. `valueDate` is typed as `LocalDate = string` in `src/lib/client/types.gen.ts:152` and is serialized by the backend as an ISO `yyyy-MM-dd` string, so it reaches the DOM unformatted.

Only the read-only display must change. The inline edit row (`TransactionDisplayRow.svelte:217-222`) and the new-transaction row (`TransactionInsert.svelte:76`) use `<input type="date">`, whose `value` attribute is required by the HTML spec to be ISO `yyyy-MM-dd`; the browser renders it in the user OS locale. Those bindings must keep the ISO string.

There is already a `src/lib/formatter.ts` holding a German `Intl.NumberFormat` (`formatEuroCents`); a date formatter belongs alongside it so other surfaces can reuse it. `src/lib/components/LogTable.svelte:52` already formats correctly via `toLocaleString` with `de-DE` and can serve as the reference behaviour.

## Defect 2 — new-transaction date field has an invalid default

`src/lib/components/TransactionInsert.svelte:32` initialises the date state with `new Date(Date.now()).toDateString()`, which yields `"Sat Aug 08 2026"`. That is not a valid `<input type="date">` value (the spec requires `yyyy-MM-dd`), so the browser rejects it and the field on line 76 renders empty instead of defaulting to today.

This is not only cosmetic. Because the input rejects the value, the `bind:value` on line 76 writes the empty string back into `date`. If the user then submits without picking a date, `submitTransaction` runs `new Date("").toISOString()` on line 44 — `new Date("")` is an Invalid Date and calling `toISOString()` on it throws a `RangeError`, so the booking fails with an unhandled exception rather than a validation message.

The default must be today in the user local timezone, formatted as ISO `yyyy-MM-dd`. Note that `new Date().toISOString().substring(0, 10)` is NOT a correct fix: it converts to UTC first, so it returns the wrong day for users whose local date differs from the UTC date at the time of use.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 The Buchungsdatum column on /app/accounts/transactions/[accountId]/ displays 2026-08-08 as "08.08.2026"
- [x] #2 Date formatting is German (dd.MM.yyyy) regardless of the browser or OS locale
- [x] #3 A reusable date formatting helper lives in src/lib/formatter.ts next to formatEuroCents
- [x] #4 Switching a row into edit mode still shows the correct date in the date picker, and saving stores the unchanged date
- [x] #5 An absent or empty valueDate renders as empty rather than "Invalid Date" or "NaN"
- [x] #6 npm run check passes with no new errors attributable to these changes
- [x] #7 The new-transaction row date field is pre-filled with today upon page load, instead of rendering empty
- [x] #8 The pre-filled default is the correct local calendar day, including for users whose local date differs from the UTC date
- [x] #9 Submitting the new-transaction form without touching the date field books the transaction with today rather than throwing a RangeError
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Goal

Two fixes on the account transactions page:
1. Render `valueDate` in the "Buchungsdatum" column as `dd.MM.yyyy` (German), via a reusable formatter.
2. Give the new-transaction date input a valid ISO default of today, so it pre-fills instead of rendering empty and cannot throw on submit.

Neither may disturb the ISO strings that the `<input type="date">` bindings require.

## Key constraint: `new Date(...)` is the hazard in both fixes

A `yyyy-MM-dd` string is a calendar date, not an instant, and the two directions fail in opposite ways:

- **Reading:** `new Date("2026-08-08")` parses as **UTC midnight**. Formatting that with a local-time formatter shifts the date back a day for users at negative UTC offsets. Fix by formatting with `timeZone: "UTC"` so parsing and formatting agree.
- **Writing:** `new Date().toISOString()` converts local time to **UTC** before truncating. Late in the day at negative offsets it yields tomorrow; early in the day at positive offsets it yields yesterday. Fix by reading the local calendar fields directly, never via `toISOString()`.

Do not "simplify" either helper into the other pattern — that is exactly how these bugs appear.

## Steps — Defect 1 (display format)

- [ ] 1. **Add the read formatter** in `src/main/frontend/src/lib/formatter.ts`, alongside `formatEuroCents`:
  - Module-level `const dateFormatter = new Intl.DateTimeFormat("de-DE", { day: "2-digit", month: "2-digit", year: "numeric", timeZone: "UTC" });` — created once, matching the existing `currencyFormatter` pattern, and locale-pinned so it ignores the browser locale (AC #2).
  - `export const formatIsoDate = (isoDate: string | null | undefined): string => { ... }`:
    - Return `""` for `null` / `undefined` / empty string (AC #5).
    - Build the date as `new Date(`${isoDate}T00:00:00Z`)` — or via `Date.UTC(...)` from split parts — so it is unambiguously UTC.
    - If `Number.isNaN(d.getTime())`, return `""` rather than `"Invalid Date"` (AC #5).
    - Otherwise `return dateFormatter.format(d);`
  - JSDoc in the style of the existing `formatEuroCents` comment.

- [ ] 2. **Use it in the display row.** In `src/main/frontend/src/lib/components/TransactionDisplayRow.svelte`:
  - Extend the import on line 11 to `import { formatEuroCents, formatIsoDate } from "$lib/formatter";`
  - Change line 146 from `<td>{dto.transaction?.valueDate}</td>` to `<td>{formatIsoDate(dto.transaction?.valueDate)}</td>`.

- [ ] 3. **Leave the edit path untouched.** No changes to `let date = $state(dto.transaction?.valueDate!)` (line 43), `allowEdit()` (line 78), the `<input type="date" bind:value={date}>` (lines 217-222), or the `new Date(date!).toISOString().substring(0, 10)` in `submitChange()` (line 97). These require ISO and must keep it (AC #4).

## Steps — Defect 2 (invalid input default)

- [ ] 4. **Add the write helper** to the same `formatter.ts`, so both directions live together:
  - `export const todayAsIsoDate = (): string => { ... }` returning todays **local** calendar date as `yyyy-MM-dd`.
  - Implement from local fields: `d.getFullYear()`, `String(d.getMonth() + 1).padStart(2, "0")`, `String(d.getDate()).padStart(2, "0")`. Explicitly do **not** use `toISOString()` (AC #8).
  - Alternative if preferred: `new Intl.DateTimeFormat("en-CA")` yields `yyyy-MM-dd` in local time. The manual version is more obvious about intent; either satisfies AC #8.
  - Add a comment stating why `toISOString()` is wrong here, so it is not "cleaned up" later.

- [ ] 5. **Fix the default** in `src/main/frontend/src/lib/components/TransactionInsert.svelte`:
  - Import the helper from `$lib/formatter` (this component has no formatter import yet; it currently imports only client functions, `error`, icons and `EuroInput`).
  - Change line 32 from `let date: string = $state(new Date(Date.now()).toDateString());` to `let date: string = $state(todayAsIsoDate());` (AC #7).

- [ ] 6. **Harden the submit conversion** in the same file, line 44. `date` is now always ISO `yyyy-MM-dd` — it comes either from `todayAsIsoDate()` or straight from the date input — so the `new Date(date).toISOString().substring(0, 10)` round-trip is redundant and is the thing that throws a `RangeError` when `date` is empty. Pass `valueDate: date` through directly (AC #9).
  - If the user can still clear the field (the input is not `required`), guard the submit: either add `required` to the input on line 76, or early-return when `date` is empty. Prefer `required` — it gives native browser validation and matches the form-driven style already used here.

## Verification

- [ ] `cd src/main/frontend && npm run check` — compare against the pre-change baseline; this repo has known pre-existing check/lint failures, so only new errors in the three touched files count (AC #6).
- [ ] `npm run build` succeeds.
- [ ] Browser check on `/app/accounts/transactions/1/` (dev UI on `:5173` sits behind Keycloak, log in as `user`/`user`):
  - Buchungsdatum column shows `08.08.2026`-style values (AC #1).
  - The new-transaction row at the bottom of the table shows todays date pre-filled, not an empty field (AC #7).
  - Click the edit (pencil) button on an existing row: the date picker is pre-filled with that rows date, not blank (AC #4). Read the input value; do not submit, to avoid writing to the dev database.
- [ ] Timezone checks — run the app or a node one-liner under a non-UTC `TZ` and confirm both directions:
  - `TZ=America/Los_Angeles`: a `2026-08-08` transaction must still render `08.08.2026`, not `07.08.2026` (AC #2).
  - `TZ=Pacific/Kiritimati` (UTC+14) and `TZ=Pacific/Midway` (UTC-11): `todayAsIsoDate()` must match the local calendar day in each, which is the case `toISOString()` gets wrong (AC #8).
- [ ] AC #9 needs a real submit, which writes to the dev database. Either verify on a throwaway account and delete the booking afterwards, or confirm by inspection that `valueDate: date` can no longer reach `toISOString()`. State in the implementation notes which route was taken.

## Notes

- There is no frontend test framework in this repo (no vitest, no `*.test.ts`), so verification is `npm run check`, `npm run build`, and the browser checks above. `formatIsoDate` and `todayAsIsoDate` are pure functions and would be the natural first unit tests if a harness is ever added — out of scope here.
- Step 4 sweep from the original plan still applies: `grep -rn "valueDate\|createdAt" --include=*.svelte src/main/frontend/src | grep -v lib/client`. Known state: `LogTable.svelte:52` already formats a datetime with `de-DE` and stays as is; `routes/app/products/tally-count/+page.svelte:54` uses `toISOString()` for a write and should be reviewed against the same timezone rule. Report before widening beyond the transactions page.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented by a Sonnet subagent, verified independently by the parent session.

## Changes

- `src/lib/formatter.ts`: added `formatIsoDate(isoDate)` (ISO yyyy-MM-dd -> German dd.MM.yyyy, returns "" for null/undefined/empty/invalid) and `todayAsIsoDate()` (todays LOCAL calendar day as ISO yyyy-MM-dd). Both sit next to the existing `formatEuroCents` and follow its module-level-formatter style.
- `src/lib/components/TransactionDisplayRow.svelte`: line 146 now renders `formatIsoDate(dto.transaction?.valueDate)`. Edit-mode ISO bindings (lines 43, 78, 97, 217-222) deliberately untouched.
- `src/lib/components/TransactionInsert.svelte`: default is now `todayAsIsoDate()`; the redundant `new Date(date).toISOString().substring(0, 10)` round-trip on submit was replaced with `valueDate: date`; the date input gained `required`.

## Key decisions

- `formatIsoDate` pins `timeZone: "UTC"` and parses via `${isoDate}T00:00:00Z` so parsing and formatting agree. Without this, a local-time formatter shifts the date back a day at negative UTC offsets.
- `todayAsIsoDate` reads local `getFullYear`/`getMonth`/`getDate` and explicitly does NOT use `toISOString()`, which converts to UTC first and returns the wrong calendar day. A code comment records this so it is not "simplified" back later.
- The submit-path `toISOString()` was removed rather than guarded: `date` is now always a valid ISO string, so the round-trip was both redundant and the source of the RangeError.

## Verification evidence

- `npm run check`: `COMPLETED 302 FILES 5 ERRORS 20 WARNINGS 15 FILES_WITH_PROBLEMS` — byte-identical to the pre-change baseline captured on the same worktree. Grepping the report for the three touched files shows zero entries for `formatter.ts` and `TransactionInsert.svelte`, and only the 5 pre-existing `state_referenced_locally` warnings at lines 41-45 of `TransactionDisplayRow.svelte` (untouched lines).
- `npm run build`: succeeded, adapter-static wrote `dist`.
- Direct execution of the real `formatter.ts` module (node --experimental-strip-types) under TZ=UTC, America/Los_Angeles, Pacific/Kiritimati, Pacific/Midway, Europe/Berlin: `formatIsoDate("2026-08-08") === "08.08.2026"` in every zone; `formatIsoDate("2026-01-05") === "05.01.2026"` (confirms dd.MM, not MM.dd); `""`/null/undefined/"not-a-date" all return `""`.
- AC #8 specifically: under TZ=Pacific/Kiritimati (UTC+14), `todayAsIsoDate()` returned `2026-08-09` while `new Date().toISOString().substring(0,10)` returned `2026-08-08` — the discrepancy the fix exists to avoid. Under Pacific/Midway the two happened to agree at this wall-clock time; recorded for honesty, Kiritimati already demonstrates it.
- AC #4/#7 via real Chromium (Playwright, `page.setContent` with `<input type="date" required>`, no backend needed): assigning the OLD default `"Sat Aug 08 2026"` gives `readBack === ""` with `validity.valueMissing === true` — the browser rejects it, which is exactly the reported empty-field defect. Assigning the NEW default `"2026-08-08"` gives `readBack === "2026-08-08"`, correct `valueAsDate`, and does not block submit. The same probe with a stored `valueDate` of `2026-08-08` confirms the edit-mode picker pre-fills correctly.

## AC #9 verification route

Per the plans stated option, this was confirmed by inspection plus the browser probe above, NOT by a live submit — the dev database was deliberately not written to. Evidence: `toISOString()` no longer appears in the `TransactionInsert` submit path, so the RangeError is structurally impossible; `date` is always a valid ISO string; and `required` makes Chromium block submission when the field is empty (`checkValidity() === false`). The POST payload shape is unchanged from before (`valueDate` was already a yyyy-MM-dd string). The end-to-end POST against a running backend was not exercised.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Transaction dates now render in German dd.MM.yyyy via a new reusable `formatIsoDate` helper in `src/lib/formatter.ts`, and the new-transaction date input defaults to a spec-valid ISO `todayAsIsoDate()` instead of `toDateString()`, which Chromium rejected outright and which made an untouched date field throw a RangeError on submit. Both helpers are timezone-correct in opposite directions: `formatIsoDate` pins `timeZone: "UTC"` so dates do not shift back a day at negative offsets, and `todayAsIsoDate` reads local calendar fields rather than `toISOString()`. Verified with `npm run check` identical to the pre-change baseline (5 errors / 20 warnings, none in the touched files), a successful `npm run build`, direct execution of the real formatter module across five timezones, and a real-Chromium probe of `<input type="date">` proving the old default was rejected and the new one is accepted. AC #9 was confirmed by inspection plus that browser probe rather than a live submit; the dev database was not written to.
<!-- SECTION:FINAL_SUMMARY:END -->
