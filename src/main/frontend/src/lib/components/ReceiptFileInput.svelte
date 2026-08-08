<script lang="ts">
	import MdiUpload from '~icons/mdi/upload';
	import MdiClose from '~icons/mdi/close-bold';

	let { files = $bindable() }: { files: FileList | undefined } = $props();
</script>

<label class="btn btn-primary h-8 w-8 p-0 text-lg">
	<MdiUpload />
	<input
		type="file"
		class="hidden"
		bind:files
		multiple={false}
		accept="image/*,application/pdf,.pdf"
	/>
</label>
<button
	class="btn btn-error h-8 w-8 p-0 text-lg"
	disabled={!(files && files.length > 0)}
	onclick={() => (files = undefined)}
>
	<MdiClose />
</button>
