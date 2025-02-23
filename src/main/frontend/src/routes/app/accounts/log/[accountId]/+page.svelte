<script lang="ts">
    import { type Account } from "$lib/client";
	import MdiPencilPlus from '~icons/mdi/pencil';
	import MdiDelete from '~icons/mdi/delete';

    const account: Account = {
        id: 1,
        name: "Flurkasse",
        active: false,
    }
</script>

<div class="inline-flex items-center w-full my-4 gap-2">
	<h1 class="text-2xl font-bold pr-2">
		{account.name}
	</h1>
    {#if account.active}
        <div class="badge badge-lg badge-primary font-bold">Aktiv</div>
    {:else}
        <div class="badge badge-lg badge-primary font-bold">Passiv</div>
    {/if}
    <div class="flex-grow"></div>
	<a href="/app/persons/create" class="btn btn-warning h-8 w-8 p-0 m-0 text-lg">
		<MdiPencilPlus/>
	</a>
    <a href="/app/persons/create" class="btn btn-error h-8 w-8 p-0 m-0 text-lg">
		<MdiDelete/>
	</a>
</div>

<div class="overflow-x-auto flex-grow rounded-box border border-base-content/5 border-slate-300 text-nowrap">
    <table class="table">
        <thead>
            <tr>
                <td>Zeitstempel</td>
                <td>Benutzer</td>
                <td>Aktion</td>
                <td>Änderung</td>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>20.02.2025,<br>15:12:01 Uhr</td>
                <td>christian</td>
                <td><div class="badge badge-success font-bold">Neu</div></td>
                <td>
                    <ul class="list-disc">
                        <li>Von: <code>Passiv:Flurkasse</code></li>
                        <li>Nach: <code>Passiv:Bewohner:Yusuf</code></li>
                        <li>Betrag: 1.000,00 €</li>
                        <li>Bezeichnung: Schutzgeld</li>
                    </ul>
                </td>
            </tr>
            <tr>
                <td>20.02.2025,<br>15:12:01 Uhr</td>
                <td>anna</td>
                <td><div class="badge badge-error font-bold">Löschen</div></td>
                <td>
                    <ul class="list-disc">
                        <li>Von: <code>Passiv:Bewohner:Christian</code></li>
                        <li>Nach: <code>Passiv:Flurkasse</code></li>
                        <li>Betrag: 0,00 €</li>
                        <li>Bezeichnung: Nix hihi</li>
                    </ul>
                </td>
            </tr>
            <tr>
                <td>20.02.2025,<br>15:12:01 Uhr</td>
                <td>anna</td>
                <td><div class="badge badge-warning font-bold">Ändern</div></td>
                <td>
                    <ul class="list-disc">
                        <li>Betrag: 0,00 €</li>
                        <li>Bezeichnung: Nix hihi</li>
                    </ul>
                </td>
            </tr>
            <tr>
                <td>20.02.2025,<br>15:12:01 Uhr</td>
                <td>anna</td>
                <td><div class="badge badge-success font-bold">Neu</div></td>
                <td>
                    <ul class="list-disc">
                        <li>Von: <code>Passiv:Bewohner:Christian</code></li>
                        <li>Nach: <code>Passiv:Flurkasse</code></li>
                        <li>Betrag: 8,00 €</li>
                        <li>Bezeichnung: Flurbeitrag 02/25</li>
                    </ul>
                </td>
            </tr>
        </tbody>
    </table>
</div>