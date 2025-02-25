<script module lang="ts">
	export const breadcrumbLabel = "Home";
</script>

<script>
	import MdiLiquor from '~icons/mdi/liquor';
	import MdiBank from '~icons/mdi/bank';
	import MdiCashClock from '~icons/mdi/cash-clock';
	import MdiAccount from '~icons/mdi/account';
	import MdiCashMultiple from '~icons/mdi/cash-multiple';
	import MdiHelp from '~icons/mdi/help';
	import MainMenuButton from '$lib/components/MainMenuButton.svelte';
</script>

<div class="max-w mx-3 mt-3">
	<div class="max-w flex flex-wrap items-center justify-center gap-3">
		<MainMenuButton redirectTo="/app/persons">
			<MdiAccount class="mx-auto h-10 w-10" />
			<h3 class="text-center text-2xl text-black">Personen</h3>
		</MainMenuButton>
        <MainMenuButton redirectTo="/app/accounts">
			<MdiBank class="mx-auto h-10 w-10" />
			<h3 class="text-center text-2xl text-black">Konten</h3>
		</MainMenuButton>
		<MainMenuButton redirectTo="/app/recurring-transactions">
			<MdiCashClock class="mx-auto h-10 w-10" />
			<h3 class="text-center text-2xl text-black">Aufträge</h3>
		</MainMenuButton>
		<MainMenuButton redirectTo="/app/products">
			<MdiLiquor class="mx-auto h-10 w-10" />
			<h3 class="text-center text-2xl text-black">Getränke</h3>
		</MainMenuButton>
		<MainMenuButton redirectTo="/app/banking">
			<MdiCashMultiple class="mx-auto h-10 w-10" />
			<h3 class="text-center text-2xl text-black">Banking</h3>
		</MainMenuButton>


		<MainMenuButton redirectTo="/app/help">
			<MdiHelp class="mx-auto h-10 w-10" />
			<h3 class="text-center text-2xl text-black">Anleitung</h3>
		</MainMenuButton>
	</div>
</div>
