<script module lang="ts">
    export const breadcrumbLabel = "Allgemeines";
</script>

<script lang="ts">
    import MdiScriptText from '~icons/mdi/script-text';
</script>

<article class="prose prose-black mt-6 max-w-none">
    <h2>Navigation</h2>
    <p>
        Am oberen Ende der Anwendung wird ein Pfad zur aktuellen Seite angezeigt. Die Seiten zwischen der Startseite und
        der aktuell angezeigten können ausgewählt werden, um zu dieser Seite zu navigieren.
    </p>
    <p>
        Auf <strong>mobilen</strong> bzw. <strong>schmalen Endgeräten</strong> kann der Text im Pfad zu lang sein, um
        vollständig auf dem Bildschirm angezeigt zu werden. In diesem Fall kann im Pfad von links nach rechts gescrollt
        werden. 
    </p>
</article>

<article class="prose prose-black mt-4 max-w-none">
    <h2>Zugriff und Benutzerkonten</h2>
    <p>
        Die Zugriffskontrolle funktioniert in RoomieFunds per OpenID-Connect. Zugriff auf alle Funktionen haben nur
        solche Konten, die die Rolle <code>roomiefunds-admin</code> besitzen. Diese Rolle muss durch den Identity
        Provider im JWT-Token an die Anwendung mitgeschickt werden.
    </p>
    <p>
        Benutzerkonten mit der Rolle <code>roomiefunds</code> können sich anmelden, sehen allerdings nur lesend das
        Saldo der Flurkasse und die Buchungen in ihrem eigenen Konto. Falls keine Person mit dem Benutzerkonto verknüpft
        ist, wird nur das Saldo der Flurkasse angezeigt.
    </p>
</article>

<article class="prose prose-black mt-4 max-w-none">
    <h2>Prüfprotokoll</h2>
    <p>
        Da RoomieFunds grundsätzlich mit Finanzdaten umgeht, ist die Nachvollziehbarkeit der Änderungen wichtig. Hierzu
        werden alle Änderungen an Daten, die von RoomieFunds gespeichert werden, protokolliert. Diese Änderungen können
        im jeweilien Modul eingesehen werden und sind überlicherweise durch einen Knopf mit dem Zeichen
        <MdiScriptText class="inline" /> erreichbar.
    </p>
    <p>
        Das Protokoll speichert das gesamte Objekt im Zustand vor- und nach der Änderung. Im Falle einer versehentlichen
        Änderung oder Löschens, kann die ehemalige Buchung, das Getränkgeprodukt, die Person, etc. abgelesen werden.
    </p>
</article>