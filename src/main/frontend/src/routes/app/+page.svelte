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
			<MdiAccount class="text-4xl" />
			Personen
		</MainMenuButton>
        <MainMenuButton redirectTo="/app/accounts">
			<MdiBank class="text-4xl" />
			Konten
		</MainMenuButton>
		<MainMenuButton redirectTo="/app/recurring-transactions">
			<MdiCashClock class="text-4xl" />
			Aufträge
		</MainMenuButton>
		<MainMenuButton redirectTo="/app/products">
			<MdiLiquor class="text-4xl" />
			Getränke
		</MainMenuButton>
		<div class="indicator">
			<span class="indicator-item badge badge-warning text-white font-bold p-0 px-1 mr-2">ToDo</span>
			<MainMenuButton redirectTo="/app/banking">
				<MdiCashMultiple class="text-4xl" />
				Banking
			</MainMenuButton>
		</div>

		<MainMenuButton redirectTo="/app/help">
			<MdiHelp class="w-10 h-10" />
			Anleitung
		</MainMenuButton>
	</div>
</div>
