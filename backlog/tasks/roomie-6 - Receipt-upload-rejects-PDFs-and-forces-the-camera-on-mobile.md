---
id: ROOMIE-6
title: Receipt upload rejects PDFs and forces the camera on mobile
status: Done
assignee:
  - '@fatih'
created_date: '2026-08-08 16:15'
updated_date: '2026-08-09 09:27'
labels: []
dependencies: []
references:
  - src/main/frontend/src/lib/components/TransactionInsert.svelte
  - src/main/frontend/src/lib/components/TransactionDisplayRow.svelte
  - >-
    src/main/java/de/flur4/roomiefunds/infrastructure/web/TransactionController.java
priority: medium
type: bug
ordinal: 8000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
A transaction can have a receipt attached. The receipt file input was built for photos only, which causes two problems in daily use.

1. PDF receipts cannot be selected. Invoices that arrive by e-mail are PDFs, but the file input declares `accept="image/*,pdf"`. `pdf` is not a valid accept token (it is neither a MIME type nor a `.pdf` extension), so the OS file picker filters PDFs out and they cannot be chosen at all. The backend already stores whatever content type is uploaded, so this is a client-side restriction.

2. On phones the upload button jumps straight into the camera. The input carries `capture="environment"`, which tells iOS and Android to skip the picker and open the rear camera immediately. The app is mostly used on phones, and users need the normal native sheet instead (iOS: Photo Library / Take Photo / Choose File; Android: the equivalent picker chooser), so an existing photo or a stored PDF can be attached.

Both file inputs are duplicated across the new-transaction row (TransactionInsert.svelte) and the edit row (TransactionDisplayRow.svelte); both must behave the same.

Viewing is expected to keep working for PDFs: the receipt is fetched as a Blob and opened in a new tab via an object URL, and browsers render PDFs natively.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A PDF file can be selected in the receipt file picker on desktop and on mobile, and uploads successfully for both a new transaction and an existing transaction
- [ ] #2 Tapping the receipt upload button on iOS shows the native action sheet (Photo Library / Take Photo / Choose File) instead of opening the camera directly; Android shows its equivalent picker chooser
- [ ] #3 Taking a photo with the camera is still possible from that sheet and still uploads correctly
- [ ] #4 An uploaded PDF receipt can be opened again from the transaction row and renders as a PDF (correct Content-Type round-trip), and image receipts still display as before
- [ ] #5 The new-transaction row and the edit row accept the same file types and show the same picker behaviour
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Root cause

Both file inputs are declared identically in two places:
- `src/main/frontend/src/lib/components/TransactionInsert.svelte` (new-transaction row, approx. lines 104-114)
- `src/main/frontend/src/lib/components/TransactionDisplayRow.svelte` (edit row, approx. lines 253-263)

```svelte
<input type="file" class="hidden" bind:files capture="environment" multiple={false} accept="image/*,pdf" />
```

- `accept="image/*,pdf"`: `pdf` is not a valid accept token, so it is ignored and only `image/*` filters. PDFs are greyed out in the picker.
- `capture="environment"`: the mere presence of the attribute (any value) makes iOS Safari and Android Chrome bypass the picker and launch the camera. Removing the attribute restores the native sheet (Photo Library / Take Photo / Choose File on iOS, the picker chooser on Android).

Nothing in the backend restricts the type: `TransactionRepositoryImpl.setTransactionReceipt` stores `fileUpload.contentType()` verbatim, and `TransactionController.getReceipt` echoes it back as the Content-Type header.

### Steps

1. Fix the input in both components:
   - delete `capture="environment"` entirely (do not set it to an empty value or bind it conditionally; no attribute at all is what triggers the native sheet)
   - change to `accept="image/*,application/pdf,.pdf"` - `application/pdf` is the correct MIME token, and the `.pdf` extension is added because some Android file providers match on extension rather than MIME
   - keep `multiple={false}` and the existing `bind:files` wiring

2. Extract the duplicated markup into `src/main/frontend/src/lib/components/ReceiptFileInput.svelte` (upload label + hidden input + clear button), taking `bind:files` as a prop, and use it in both rows. This is what makes AC #5 hold over time instead of by coincidence; skip only if the shared component conflicts with the surrounding table layout.

3. Upload path needs no change, but confirm it: both callers post through `postApiTransactionByTransactionIdReceipt({ body: { receipt: files[0] } })`. The browser sets the multipart part Content-Type from the File type, so `application/pdf` reaches `fileUpload.contentType()` and is persisted into `transaction.receipt_mime_type`.

4. Verify the view path for PDFs end to end. `showReceipt()` in TransactionDisplayRow fetches the receipt and calls `openPDFInNewTab()`, which does `URL.createObjectURL(blob)`. The generated client treats any `application/*` response as a Blob (`src/main/frontend/src/lib/client/client/utils.gen.ts:166`), so a PDF already lands as a Blob. Check in the browser that the resulting Blob carries `type: "application/pdf"`; if it comes back untyped the new tab downloads instead of rendering, and the fix is to rebuild the Blob with the response Content-Type before creating the object URL.

5. Add a server-side allow-list in `TransactionService.setTransactionReceipt`: accept only `image/*` and `application/pdf`, otherwise reject with 400. The content type is client-supplied today and is echoed back unchanged on GET, and the frontend opens that response in a new tab from an object URL - an uploaded `text/html` would therefore execute in the app origin context. Widening the accepted types is the right moment to close this. Flagged as a decision: say so if this should be split into its own ticket instead.

6. Check the upload size ceiling for PDFs. `application.properties` sets no `quarkus.http.limit.max-body-size`, so the Quarkus default of 10M applies. Scanned invoices usually fit, but confirm the limit is intentional and that exceeding it surfaces a readable error rather than a silent failure (both callers currently only `console.error` the upload error).

### Verification

- Frontend: run the checks scoped to the touched files. The repo-wide `npm run check` / `npm run lint` are already red on pre-existing issues, so compare against the baseline instead of expecting a clean run.
- Desktop manual test at the dev UI on :5173 (Keycloak login user/user): attach a PDF to a new transaction and to an existing one, reopen both, confirm they render as PDFs; re-test an image receipt for regression. Use a throwaway account/transaction, since this test must actually write to the dev DB.
- Mobile behaviour (AC #2, #3) cannot be proven in a desktop browser. Verify in DevTools that the rendered input has no `capture` attribute and the expected `accept` value, then confirm the sheet on a real iOS and Android device.

### Addendum: the view/download button on mobile

The download path was verified to be type-correct: the generated client calls `response.blob()`, so the Blob inherits the server Content-Type and the object URL renders a PDF inline. No client parsing change is needed. Two mobile-specific risks in `showReceipt()` were missed in the original plan, however - both pre-existing, both more likely to bite with PDFs than with images.

7. Popup blocking after await. `showReceipt()` awaits the fetch and only then calls `window.open`. By that point the user activation from the click has expired, so iOS Safari blocks the new tab and the button appears to do nothing. Fix by opening the tab synchronously inside the click handler and assigning `newTab.location = url` once the blob resolves, or by triggering a hidden anchor instead. Confirm the current behaviour on a real device before changing it - if it already works, leave it alone.

8. Blob URLs in a new tab are unreliable for PDFs on iOS Safari, which may render a blank page where the same URL renders fine on desktop. If that reproduces, fall back to an `<a download>` with an explicit filename, or have the backend send `Content-Disposition: inline; filename=...` on GET `/api/transaction/{id}/receipt`.

9. No filename is stored. `V0011__Add_receipt_fields_in_transaction.sql` added only `receipt` and `receipt_mime_type`, so anything saved out of the viewer is named after the blob UUID with no extension. Adding a `receipt_file_name` column plus persisting `fileUpload.fileName()` would fix it. Out of scope for this bug unless requested - raise it as a follow-up ticket.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented plan steps 1, 2, 5 and 7. Steps 3, 4 and 6 were verification-only and needed no code change; steps 8 and 9 were left out of scope as planned.

- New shared component `src/main/frontend/src/lib/components/ReceiptFileInput.svelte` holds the upload label, hidden input and clear button, and is used by both TransactionInsert.svelte and TransactionDisplayRow.svelte. This removes the duplication that let the two rows drift apart.
- `capture="environment"` is gone entirely (`grep -rn capture src/main/frontend/src` returns nothing), so mobile browsers show the native picker sheet instead of jumping into the camera.
- `accept` is now `image/*,application/pdf,.pdf`. The `.pdf` extension is included because some Android file providers match on extension rather than MIME type.
- Backend allow-list: new `InvalidReceiptContentTypeException`, thrown from `TransactionService.setTransactionReceipt` before any repository write when the content type is null, blank, or not `image/*` / `application/pdf` (compared lowercased with any `;charset=...` suffix stripped). `TransactionController.storeReceipt` catches it ahead of the generic handler and maps it to HTTP 400 instead of 500.
- `showReceipt()` now opens the tab synchronously inside the click handler and assigns `newTab.location.href` once the blob resolves, so iOS Safari does not block it after the await. Falls back to the old behaviour if the popup is blocked anyway, and closes the pre-opened tab on both an empty response and a thrown error.

Verification: frontend `npm run check` 5 errors / 20 warnings and `npm run lint` 138 problems, both identical to the pre-change baseline, with no problems in the three touched files. Prettier clean. Backend recompiled from scratch with JDK 21 (`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./mvnw -Djooq compile`), 243 sources, BUILD SUCCESS. JDK 25 is the machine default and breaks the build, so JDK 21 is required.

Not verified: acceptance criteria #2 and #3 need a real iOS and Android device, and the PDF round-trip in #1 and #4 needs a manual run against the dev stack.
<!-- SECTION:NOTES:END -->
