<script>
	import MdiLiquor from '~icons/mdi/liquor';
	import MdiBank from '~icons/mdi/bank';
	import MainMenuButton from '$lib/components/MainMenuButton.svelte';
</script>

<div class="max-w mx-3 mt-3">
	<div class="max-w flex flex-wrap items-center justify-center gap-3">
		<MainMenuButton redirectTo="/app/products">
			<MdiLiquor class="mx-auto h-10 w-10" />
			<h3 class="text-center text-2xl text-black">Produkte</h3>
		</MainMenuButton>
        <MainMenuButton redirectTo="/app/accounts">
			<MdiBank class="mx-auto h-10 w-10" />
			<h3 class="text-center text-2xl text-black">Konten</h3>
		</MainMenuButton>
	</div>
</div>
