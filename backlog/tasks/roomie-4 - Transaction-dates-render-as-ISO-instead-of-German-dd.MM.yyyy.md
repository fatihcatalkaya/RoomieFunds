---
id: ROOMIE-4
title: >-
  Transaction date display uses ISO format and new-transaction date default is
  invalid
status: Done
assignee:
  - '@fatih'
created_date: '2026-08-08 16:11'
updated_date: '2026-08-08 16:30'
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
- [x] #10 Striche zählen (/app/products/tally-count/) books with the local calendar day, not the UTC-converted day
- [x] #11 Clearing the date in an existing transaction edit row cannot submit an empty value or throw a RangeError
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

---

## Follow-up round 2 — remaining `toISOString()` date writes

Added after PR #171 review: the follow-up flagged in that PR is being folded into this ticket rather than filed separately.

A full sweep of the frontend for date construction/serialization (`grep -rn "toISOString\|toDateString\|toLocaleDateString\|toLocaleString\|new Date(" --include=*.svelte --include=*.ts src | grep -v lib/client`) found exactly three non-generated sites. Two are defective, one is fine.

### Site A — `routes/app/products/tally-count/+page.svelte:54` (AC #10)

`valueDate: new Date(Date.now()).toISOString().substring(0, 10)` — the same UTC-conversion bug already fixed in `TransactionInsert`. This page has no date input at all; the booking date is implicitly "today", so the wrong day is silently written with nothing for the user to correct.

This bites the actual target audience. Reproduced under `TZ=Europe/Berlin` (UTC+2): at the instant `2026-08-08T22:30:00Z`, which is `2026-08-09 00:30` local, `toISOString().substring(0,10)` yields `2026-08-08` while the local calendar day is `2026-08-09`. Every tally booked in the first one-to-two hours after local midnight is dated to the previous day.

- [ ] Replace with `valueDate: todayAsIsoDate()`.
- [ ] The file already imports `formatEuroCents` from `$lib/formatter` on line 9 — extend that existing import rather than adding a second one.

### Site B — `lib/components/TransactionDisplayRow.svelte:97` (AC #11)

`valueDate: new Date(date!).toISOString().substring(0, 10)` in `submitChange`. Here `date` originates from an ISO source (the stored `valueDate`, or the `<input type="date">`), so `new Date(iso)` is UTC midnight and the round-trip returns the same day in every timezone. The timezone behaviour is therefore NOT broken.

It is still worth fixing for two reasons:
1. The round-trip is redundant — `date` is already exactly the `yyyy-MM-dd` the API wants.
2. It carries the identical latent crash that defect 2 had: the edit-row date input on lines 217-222 has no `required`, so a user can clear it, and `new Date("").toISOString()` throws `RangeError: Invalid time value` (verified). The insert row was hardened against this; the edit row was not.

- [ ] Replace with `valueDate: date`.
- [ ] Add `required` to the edit-row date input, matching the insert row.

### Site C — `lib/components/LogTable.svelte:52` — no change

`new Date(entry.createdAt!).toLocaleString("de-DE")` formats a full timestamp, not a calendar date. `createdAt` is an instant, so local-time conversion is correct here, and the output is already German. Leave as is.

### Verification for this round

- [ ] `npm run check` still matches the 5 ERRORS / 20 WARNINGS baseline, with no new entries for `tally-count/+page.svelte` (it has 2 pre-existing errors and 1 pre-existing warning — the count must not grow).
- [ ] `npm run build` succeeds.
- [ ] Re-run the Berlin repro and confirm `todayAsIsoDate()` tracks the local calendar day where `toISOString()` did not.
- [ ] Browser probe confirming the edit-row date input now blocks submission when cleared (`checkValidity() === false`), the same evidence used for the insert row.
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

## Round 2 — remaining `toISOString()` date writes (folded in after PR #171 review)

Swept the whole frontend for date construction/serialization; three non-generated sites, two defective.

- `routes/app/products/tally-count/+page.svelte:54` — now `valueDate: todayAsIsoDate()`, extending the existing `$lib/formatter` import on line 9. This page has no date input, so the wrong day was written silently with nothing for the user to correct. Reproduced the defect under `TZ=Europe/Berlin` at instant `2026-08-08T22:30:00Z` (= `2026-08-09 00:30` local): `toISOString().substring(0,10)` gave `2026-08-08` against a local calendar day of `2026-08-09`. Every tally booked in the first one-to-two hours after local midnight was dated to the previous day.
- `lib/components/TransactionDisplayRow.svelte:97` — now `valueDate: date`, plus `required` on the edit-row date input. The timezone behaviour here was NOT broken (`date` is already ISO, so the round-trip returned the same day in every zone), but the round-trip was redundant and carried the same latent crash as defect 2: the edit input had no `required`, and `new Date("").toISOString()` throws `RangeError: Invalid time value` (verified directly).
- `lib/components/LogTable.svelte:52` — deliberately unchanged. It formats an instant (`createdAt`) as a full timestamp, so local-time conversion is correct and the output is already German.

### Round 2 verification evidence

- `npm run check`: `COMPLETED 302 FILES 5 ERRORS 20 WARNINGS 15 FILES_WITH_PROBLEMS` — still identical to the original pre-change baseline. The 2 pre-existing errors and 1 pre-existing warning in `tally-count/+page.svelte` (`Property "name" does not exist on type "Person"`, `state_referenced_locally`) are unrelated to the edited line and did not grow.
- `npm run build`: succeeded.
- `npx prettier --check` on all four touched files: "All matched files use Prettier code style!"
- Chromium probe of the edit-row input as changed (`type="date" required` inside a form): loading a stored `2026-08-08` gives `blocksSubmit: false`; clearing it gives `readBack: ""`, `valueMissing: true`, `blocksSubmit: true`. The empty value can no longer reach the submit handler, so the `RangeError` is unreachable (AC #11).
- Confirmed no `toISOString()` remains anywhere in `src/main/frontend/src` outside the generated `lib/client/` serializers and the explanatory comment in `formatter.ts`.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Transaction dates now render in German dd.MM.yyyy via a new reusable `formatIsoDate` helper in `src/lib/formatter.ts`, and every date the frontend writes goes through `todayAsIsoDate()` or an already-ISO value instead of `toISOString()`/`toDateString()`.

Four defects fixed across four files:
1. The Buchungsdatum column rendered the raw ISO `valueDate` instead of `08.08.2026`.
2. The new-transaction row defaulted to `toDateString()`, which Chromium rejects outright, leaving the field empty and making an untouched date throw a RangeError on submit.
3. Striche zählen wrote `toISOString()`-derived dates with no date input to correct them, misdating every tally booked in the first one-to-two hours after local midnight (reproduced under Europe/Berlin).
4. The transaction edit row carried the same redundant round-trip and the same latent RangeError, with no `required` on its date input.

The two helpers are timezone-correct in opposite directions: `formatIsoDate` pins `timeZone: "UTC"` so dates do not shift back a day at negative offsets, and `todayAsIsoDate` reads local calendar fields rather than `toISOString()`, which converts to UTC first.

Verified with `npm run check` identical to the pre-change baseline (5 errors / 20 warnings, none newly attributable to the touched files), a successful `npm run build`, prettier clean on all four files, direct execution of the real formatter module across five timezones, a Europe/Berlin repro of the tally-count misdating, and Chromium probes of `<input type="date">` proving the old default was rejected, the new one is accepted, and a cleared edit-row date now blocks submission. No `toISOString()` remains outside the generated client.

AC #9 was confirmed by inspection plus browser probe rather than a live submit; the dev database was never written to, so an end-to-end booking smoke test is still worth doing before merge.
<!-- SECTION:FINAL_SUMMARY:END -->
