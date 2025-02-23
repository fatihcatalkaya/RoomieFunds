<script module>
	export const breadcrumbLabel = 'Konten';
</script>

<script lang="ts">
	import { formatEuroCents } from '$lib/formatter';

    import MdiPlus from '~icons/mdi/plus';
</script>

<div class="my-4 inline-flex w-full items-center">
	<h1 class="text-2xl font-bold pr-6">
        Konten
    </h1>
    <div class="flex-1 md:flex-0"></div>
    <label class="inline-flex gap-2 items-center">
        <input type="checkbox" class="toggle" />
        <span>
            Inaktiv<span class="hidden md:inline">e Konten anzeigen</span>
        </span>
    </label>
    <div class="flex-2"></div>
	<a href="/app/accounts/create" class="btn btn-success m-0 h-8 w-8 p-0 text-lg">
		<MdiPlus />
	</a>
</div>

<ul class="menu bg-base-200 rounded-box w-full">
	<li>
		<h2 class="menu-title text-neutral">Aktiv</h2>
		<ul>
			<li><a>Barkasse</a></li>
			<li><a>Bankkonto</a></li>
			<li><a>PayPal</a></li>
		</ul>
        <h2 class="menu-title text-neutral">Passiv</h2>
		<ul>
            <li>
                <a href="/app/accounts/transactions/1" class="inline-flex">
                    <span class="flex-1">Flurkasse</span>
                    <span>Saldo: {formatEuroCents(477)}</span>
                </a>
            </li>
			<li>
                <h2 class="menu-title text-neutral">Bewohner</h2>
                <ul>
                    <li><a>Yusuf R403</a></li>
                    <li><a>Fatih R405</a></li>
                    <li><a>Christian R406</a></li>
                </ul>
            </li>
			<li><a>Getränkekasse</a></li>
		</ul>
	</li>
</ul>
