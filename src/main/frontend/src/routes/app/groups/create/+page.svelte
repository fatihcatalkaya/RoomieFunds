<script module lang="ts">
	export const breadcrumbLabel = 'Neu';
</script>

<script lang="ts">
	import { goto } from '$app/navigation';
	import { postApiGroup, type CreateGroupDto } from '$lib/client';

	let newGroup: CreateGroupDto = $state({
		name: '',
		keycloakGroupId: ''
	});

	async function createGroup() {
		let query = await postApiGroup({ body: { ...newGroup } });
		if (query.error) {
			console.error(query.error);
		} else {
			goto('../');
		}
		return false;
	}
</script>

<div class="inline-flex items-center w-full my-4">
	<h1 class="flex-grow text-2xl font-bold">Neue Gruppe</h1>
</div>

<form class="mx-auto grid max-w-md grid-cols-1 gap-2" onsubmit={createGroup}>
	<label class="flex w-full items-center">
		<span class="w-1/4">Name</span>
		<input type="text" class="input w-3/4" placeholder="floor-members" minlength="1" required bind:value={newGroup.name} />
	</label>
	<label class="flex w-full items-center">
		<span class="w-1/4">KC Group ID</span>
		<input type="text" class="input w-3/4" placeholder="(optional)" bind:value={newGroup.keycloakGroupId} />
	</label>
	<div class="mt-2 join grid grid-cols-2">
		<button class="btn order-1 btn-success join-item">Speichern</button>
		<a href="/app/groups" class="btn order-0 join-item">Zurück</a>
	</div>
</form>
