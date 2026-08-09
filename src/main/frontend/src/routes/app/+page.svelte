<script module lang="ts">
	export const breadcrumbLabel = 'Home';
</script>

<script>
	import MdiLiquor from '~icons/mdi/liquor';
	import MdiBank from '~icons/mdi/bank';
	import MdiCashClock from '~icons/mdi/cash-clock';
	import MdiCashMultiple from '~icons/mdi/cash-multiple';
	import MdiAccount from '~icons/mdi/account';
	import MdiAccountGroup from '~icons/mdi/account-group';
	import MdiHelp from '~icons/mdi/help';
	import MainMenuButton from '$lib/components/MainMenuButton.svelte';
</script>

<div class="max-w mx-3 mt-3">
	<div class="max-w flex flex-wrap items-center justify-center gap-3">
		<MainMenuButton redirectTo="/app/persons">
			<MdiAccount class="text-4xl" />
			Personen
		</MainMenuButton>
		<MainMenuButton redirectTo="/app/groups">
			<MdiAccountGroup class="text-4xl" />
			Gruppen
		</MainMenuButton>
		<MainMenuButton redirectTo="/app/accounts">
			<MdiBank class="text-4xl" />
			Konten
		</MainMenuButton>
		<MainMenuButton redirectTo="/app/recurring-transactions">
			<MdiCashClock class="text-4xl" />
			Aufträge
		</MainMenuButton>
		<MainMenuButton redirectTo="/app/flurbeitrag">
			<MdiCashMultiple class="text-4xl" />
			Flurbeitrag
		</MainMenuButton>
		<MainMenuButton redirectTo="/app/products">
			<MdiLiquor class="text-4xl" />
			Getränke
		</MainMenuButton>
		<MainMenuButton redirectTo="/app/help">
			<MdiHelp class="h-10 w-10" />
			Anleitung
		</MainMenuButton>
	</div>
</div>
