---
id: ROOMIE-7
title: Add main menu entry for editing the Flurbeitrag amount
status: Done
assignee: []
created_date: '2026-08-09 08:54'
updated_date: '2026-08-09 09:17'
labels:
  - frontend
dependencies: []
references:
  - src/main/frontend/src/routes/app/+page.svelte
  - src/main/frontend/src/lib/components/EuroInput.svelte
  - src/main/frontend/src/lib/components/MainMenuButton.svelte
  - >-
    src/main/java/de/flur4/roomiefunds/infrastructure/web/FlurbeitragController.java
  - src/main/resources/db/migration/V0008__Default_values_for_settings.sql
priority: medium
type: feature
ordinal: 7000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Der monatliche Flurbeitrag kann aktuell nur direkt in der Datenbank geaendert werden. In der Tabelle `settings` liegt er unter dem Schluessel `flurbeitrag_amount` in der Spalte `value_int` (Betrag in Cent, wie alle Geldbetraege im Backend; Default aus V0008 ist 8).

Das Backend bietet dafuer bereits eine fertige, admin-geschuetzte API an: `GET /api/flurbeitrag` und `PUT /api/flurbeitrag` (FlurbeitragController, RolesAllowed roomiefunds-admin, Body: `{ "flurbeitrag": <cent> }`, validiert mit @PositiveOrZero). Jede Aenderung wird ueber FlurbeitragService in der Log-Tabelle mit Vorher-/Nachher-Wert protokolliert. Im Frontend existiert dazu bisher keine Oberflaeche - die generierten Clients `getApiFlurbeitrag` und `putApiFlurbeitrag` in `src/main/frontend/src/lib/client/sdk.gen.ts` sind vorhanden, werden aber nirgends verwendet.

Ziel ist eine neue Kachel im Hauptmenue (`/app`, MainMenuButton wie Personen/Gruppen/Konten), die auf eine neue Seite fuehrt, auf der der aktuelle Flurbeitrag angezeigt und geaendert werden kann. Der Betrag wird ueber die bestehende EuroInput-Komponente eingegeben (arbeitet in Cent, negative Werte sind hier nicht erlaubt). Ohne diese Oberflaeche muss ein Admin fuer eine Beitragsanpassung psql benutzen; der FlurbeitragScheduler bucht den Betrag monatlich automatisch allen Personen mit "Bezahlt Flurbeitrag" ab.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Auf der Hauptmenue-Seite /app gibt es eine neue MainMenuButton-Kachel "Flurbeitrag" mit passendem MDI-Icon, die auf die neue Seite verlinkt
- [x] #2 Die neue Seite laedt beim Oeffnen den aktuellen Flurbeitrag ueber getApiFlurbeitrag und zeigt ihn in einem EuroInput-Feld an
- [x] #3 Der Betrag kann geaendert und gespeichert werden; das Speichern ruft putApiFlurbeitrag mit dem Wert in Cent auf
- [x] #4 Negative Betraege koennen nicht gespeichert werden (EuroInput ohne allowNegative, passend zur @PositiveOrZero-Validierung des Backends)
- [x] #5 Nach erfolgreichem Speichern erhaelt der Nutzer eine sichtbare Rueckmeldung; API-Fehler (inkl. 403 fuer Nicht-Admins) werden ueber ErrorAlert angezeigt und verwerfen die Eingabe nicht
- [x] #6 Die Seite exportiert einen breadcrumbLabel, sodass die Breadcrumb-Navigation "Flurbeitrag" anzeigt
- [x] #7 Texte sind auf Deutsch und folgen dem Stil der bestehenden Seiten
- [x] #8 Erfolgs- und Fehlermeldungen werden oberhalb des Formulars angezeigt, nicht darunter
- [x] #9 Das Formular enthaelt ein Auswahlfeld (select) fuer das Flurkonto, das mit den Konten aus getApiAccount befuellt und mit dem aktuell konfigurierten Flurkonto vorbelegt wird
- [x] #10 Beim Speichern wird eine geaenderte Flurkonto-Auswahl ueber putApiKontoFlurkonto persistiert und ist nach einem Reload weiterhin ausgewaehlt
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. **Neue Route anlegen** `src/main/frontend/src/routes/app/flurbeitrag/+page.ts`: `export const prerender = false;` (Root-Layout setzt `prerender = true`/`ssr = false`, datenladende Seiten wie `products/edit/[id]/+page.ts` schalten Prerendering ab) und ein `PageLoad`, das `getApiFlurbeitrag()` aufruft und das komplette Query-Ergebnis (`{ data, error, response }`) zurueckgibt.

2. **Seite bauen** `src/main/frontend/src/routes/app/flurbeitrag/+page.svelte`:
   - `<script module lang="ts">` mit `export const breadcrumbLabel = "Flurbeitrag";` (AC 6, Breadcrumb.svelte liest diesen Modul-Export).
   - Daten via `PageProps` lesen; Ladefehler (inkl. 403) in einem `{#if}`-Zweig mit `ErrorAlert` melden ("Konnte den Flurbeitrag nicht laden!"), analog zu `accounts/transactions/[accountId]/+page.svelte`.
   - Betrag als `$state` aus `data...flurbeitrag ?? 0` initialisieren; der generierte Typ `Flurbeitrag` hat `flurbeitrag?: number`, also Fallback noetig.
   - Formular im Stil von `products/edit/[id]/+page.svelte`: `form method="dialog" class="mx-auto grid max-w-md grid-cols-1 gap-2"`, ein `label` mit Beschriftung "Flurbeitrag" und `<EuroInput class="input w-3/4" bind:value={amount} />` ohne `allowNegative` (AC 4; EuroInput rechnet intern Cent <-> Euro).
   - `join`-Buttonleiste unten: "Zurueck" verlinkt auf `/app` (kein Listen-Parent vorhanden), "Speichern" als `btn btn-success` mit Submit-Handler.

3. **Speichern implementieren**: Handler ruft `putApiFlurbeitrag({ body: { flurbeitrag: amount } })`. Anders als die Edit-Seiten wird *nicht* per `goto` weiternavigiert, da es keine Uebersichtsliste gibt. Stattdessen:
   - Erfolg -> `saveError = null`, sichtbarer `alert alert-success`-Banner ("Flurbeitrag gespeichert.") (AC 5).
   - Fehler -> `saveError` setzen, `ErrorAlert` rendern (bei `response.status === 403` Hinweis auf fehlende Admin-Rolle), Eingabewert unveraendert lassen (AC 5).
   - Speichern-Button waehrend des Requests deaktivieren, damit keine Doppel-PUTs entstehen.

4. **Menue-Kachel ergaenzen** in `src/main/frontend/src/routes/app/+page.svelte`: Import eines MDI-Icons (Vorschlag `~icons/mdi/cash-multiple`, ueber `@iconify-json/mdi` verfuegbar) und ein `<MainMenuButton redirectTo="/app/flurbeitrag">` mit Label "Flurbeitrag", eingeordnet nach "Auftraege" (AC 1).

5. **Kein Backend-Aenderungsbedarf**: `FlurbeitragController` (GET/PUT), `FlurbeitragService` inkl. Log-Eintrag und die generierten SDK-Funktionen existieren bereits. `sdk.gen.ts`/`types.gen.ts` NICHT neu generieren.

6. **Verifikation**: `npx prettier --write` auf die drei geaenderten/neuen Dateien; `npm run check` laufen lassen und nur die eigenen Dateien bewerten (Repo hat vorbestehende rote Quality-Gates); manueller Durchlauf gegen die Dev-UI auf `:5173` (Login `user`/`user`, dieser Account ist in der Keycloak-Gruppe `/roomiefunds-admin`): Kachel sichtbar, Wert wird geladen, Speichern zeigt Erfolgsmeldung, negativer Wert nicht eingebbar. Vor dem Test den aktuellen Wert per `docker compose exec -T db psql -U app -d app -c "SELECT * FROM settings"` sichern und danach zuruecksetzen, damit die Dev-DB sauber bleibt.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Umgesetzt wie geplant, mit zwei Abweichungen, die sich erst beim Browser-Test gezeigt haben:

1. **`quarkus.http.cors.methods` enthielt kein `PUT`** (`src/main/resources/application.properties`). Der erste Speicherversuch aus der Dev-UI wurde mit `403 CORS Rejected - Invalid method` abgewiesen. `FlurbeitragController` und `FlurkontoController` sind die einzigen `@PUT`-Endpunkte und hatten bisher keinen Frontend-Consumer, deshalb ist das nie aufgefallen. `PUT` wurde der Liste hinzugefuegt; danach liefert der Request `204 No Content`. Betrifft nur die Dev-Trennung von :5173 und :8080 - im Build liegt das SPA same-origin im Quarkus-App.

2. **`EuroInput` verhindert negative Eingaben nicht.** `allowNegative={false}` schaltet nur die eigene Vorzeichen-Behandlung der Komponente ab; ein getipptes Minus bleibt im darunterliegenden intl-number-input stehen (Feld zeigte `-12,34 €`). Statt die gemeinsam genutzte Komponente anzufassen, prueft die Seite den Wert vor dem PUT und zeigt "Der Flurbeitrag darf nicht negativ sein." - der Request wird gar nicht erst abgeschickt.

Weitere Umsetzungsentscheidungen:
- Nach dem Speichern wird nicht per `goto` navigiert (es gibt keine Uebersichtsliste), sondern ein `alert alert-success` gezeigt. Der gespeicherte Betrag wird in `savedAmount` gemerkt, damit das Banner beim naechsten Tippen wieder verschwindet.
- Ein geleertes Feld (EuroInput liefert dann `null`) wird als 0 gespeichert und im Feld auf `0,00 €` normalisiert.
- `sdk.gen.ts`/`types.gen.ts` wurden nicht neu generiert; das Backend ausser der CORS-Zeile nicht angefasst.

Validierung: `npx prettier --check` sauber; `npm run check` meldet fuer die neuen Dateien 0 Fehler (die 5 Fehler und der Rest der Warnungen sind vorbestehend), lediglich die im ganzen Repo uebliche `state_referenced_locally`-Warnung beim Destrukturieren von `data`. `npx eslint` meldet fuer die neue Seite nur `svelte/no-navigation-without-resolve` - dieselbe Regel schlaegt bei jedem bestehenden Link/`goto` im Repo an, `resolve()` wird nirgends verwendet.

Nachtrag (Folgewunsch): Meldungen ueber das Formular verschoben und Flurkonto-Auswahl ergaenzt.

- Erfolgs- und Fehlermeldung stehen jetzt zwischen Einleitungstext und Formular. Beide teilen sich denselben Platz, damit die Seite nicht springt.
- `+page.ts` laedt zusaetzlich `getApiKontoFlurkonto` und `getApiAccount` parallel per `Promise.all`. Schlaegt einer der drei Requests fehl, greift der gemeinsame Fehlerzweig ("Konnte die Flurbeitrag-Einstellungen nicht laden!").
- Das Select ist mit dem konfigurierten Flurkonto vorbelegt. Ist noch keins gesetzt (`flur_account_id = 0` -> Backend liefert leeres Optional), steht eine deaktivierte Platzhalter-Option "Kein Flurkonto ausgewaehlt" davor.
- Speichern schreibt nur die tatsaechlich geaenderten Werte: `putApiFlurbeitrag` bei geaendertem Betrag, `putApiKontoFlurkonto` bei geaenderter Kontoauswahl. Beide Endpunkte protokollieren jede Aenderung, deshalb werden unveraenderte Werte nicht erneut gesendet. Schlaegt der erste Request fehl, wird der zweite nicht mehr abgeschickt und die Fehlermeldung benennt das betroffene Feld.

**Fund: der generierte Client kann `PUT /api/konto/flurkonto` nicht aufrufen.** Der Endpunkt nimmt einen nackten `Long` entgegen, wofuer der Generator `bodySerializer: null` erzeugt. `client.gen.ts` sendet aber ausschliesslich `opts.serializedBody`, das ohne Serializer nie gesetzt wird - der Request geht komplett ohne Body raus, der Client entfernt zusaetzlich den `Content-Type`-Header ("remove Content-Type header if body is empty"), und Quarkus antwortet mit 415. Per curl mit `text/plain` bzw. `application/json` und Body `3` liefert derselbe Endpunkt 200. Workaround an der Aufrufstelle: `bodySerializer: (id: number) => String(id)`. Betrifft genauso `putApiKontoGetraenkekonto` (gleiche Signatur), sobald das mal ein Frontend nutzt - sauberer waere eine Aenderung der Backend-Signatur auf ein DTO.

Nachtrag: Meldungsbereich sitzt jetzt direkt unter der Ueberschrift, also auch oberhalb des Beschreibungstexts. Im Snapshot bestaetigt: heading -> alert -> paragraph -> form. AC 8 bleibt damit erfuellt.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Neue Seite `/app/flurbeitrag` (Betrag via EuroInput + Flurkonto-Auswahl), erreichbar ueber eine neue "Flurbeitrag"-Kachel im Hauptmenue. Meldungen stehen oberhalb des Formulars. Zwei Blocker mussten mitbehoben werden: `PUT` fehlte in `quarkus.http.cors.methods`, und der generierte Client sendet fuer `PUT /api/konto/flurkonto` (nackter Long, `bodySerializer: null`) gar keinen Body - dafuer wird an der Aufrufstelle ein `bodySerializer` mitgegeben.

Verifiziert im Browser gegen die Dev-UI auf :5173 (Login `user`, Keycloak-Gruppe `/roomiefunds-admin`), jeweils gegen die Docker-Compose-DB gegengeprueft:
- Kachel im Menue vorhanden und verlinkt auf /app/flurbeitrag; Breadcrumb zeigt "Home > Flurbeitrag".
- Laden: Feld zeigt `10,00 €` bei `flurbeitrag_amount = 1000`; Select listet alle drei Konten aus `getApiAccount` und steht bei `flur_account_id = 0` auf der deaktivierten Platzhalter-Option.
- Nur Konto geaendert: ein einziger `PUT /api/konto/flurkonto` 200, `flur_account_id = 3`; nach Reload ist "Aktiv:Bankkonto" weiterhin vorausgewaehlt.
- Betrag und Konto gemeinsam geaendert: `PUT /api/flurbeitrag` 204 und `PUT /api/konto/flurkonto` 200, DB danach `flurbeitrag_amount = 1150`, `flur_account_id = 2`.
- Meldungen: Erfolgs-Banner und Fehlermeldung erscheinen im Snapshot zwischen Einleitungstext und Formular (AC 8); Banner verschwindet beim naechsten Tippen.
- Negativ: Eingabe `-5,00` -> "Der Flurbeitrag darf nicht negativ sein." oberhalb des Formulars, kein PUT im Request-Log.
- Fehlerfall 403 (gemockt): Rollen-Hinweis, Eingabe bleibt erhalten, DB unveraendert.

DB nach dem Test auf den Ausgangsstand zurueckgesetzt (`flurbeitrag_amount = 1000`, `flur_account_id = 0`).
<!-- SECTION:FINAL_SUMMARY:END -->
