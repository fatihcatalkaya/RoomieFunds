<script lang="ts">
	import type { LogEntryDto } from "$lib/client";

    type LogTableProps = {
        logEntries: LogEntryDto[]
    }

    const { logEntries }: LogTableProps = $props();

    type Diff<T> = {
		[K in keyof T]?: T[K] | null;
	};

	function diffObjects<T extends object>(obj1: T, obj2: T): Diff<T> {
		const diffResult: Diff<T> = {};

		// Check for added or changed keys
		for (const key in obj1) {
			if (!(key in obj2)) {
				diffResult[key] = null; // Key was deleted in obj2
			} else if (obj1[key] !== obj2[key]) {
				diffResult[key] = obj2[key]; // Key value changed in obj2
			}
		}

		// Check for keys that are in obj2 but not in obj1 (added keys)
		for (const key in obj2) {
			if (!(key in obj1)) {
				diffResult[key] = obj2[key]; // New key in obj2
			}
		}

		return diffResult;
	}
</script>

<div class="rounded-box border-base-content/5 flex-grow overflow-x-auto border border-slate-300 text-nowrap">
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
            {#each logEntries! as entry}
                <tr>
                    <td>{new Date(entry.createdAt!).toLocaleString("de-DE")}</td>
                    <td>{entry.modifiedBy!.split(' ').slice(1).join(' ')}</td>
                    <td>
                        {#if entry.logOperation == 'create'}
                            <div class="badge badge-success font-bold">Neu</div>
                        {:else if entry.logOperation == 'update'}
                            <div class="badge badge-warning font-bold">Ändern</div>
                        {:else if entry.logOperation == 'delete'}
                            <div class="badge badge-error font-bold">Löschen</div>
                        {/if}
                    </td>
                    <td>
                        <ul class="list-disc">
                            {#if entry.logOperation == "create"}
                                {#each Object.entries(JSON.parse(entry.subjectAfterJson!)) as [key, value]}
                                    <li>{key}: {value}</li>
                                {/each}
                            {:else if entry.logOperation == "update"}
                                {#each Object.entries(diffObjects(JSON.parse(entry.subjectBeforeJson!), JSON.parse(entry.subjectAfterJson!))) as [key, value]}
                                    <li>{key}: {value}</li>
                                {/each}
                            {:else if entry.logOperation == "delete"}
                                {#each Object.entries(JSON.parse(entry.subjectBeforeJson!)) as [key, value]}
                                    <li>{key}: {value}</li>
                                {/each}
                            {/if}
                        </ul>
                    </td>
                </tr>
            {/each}
        </tbody>
    </table>
</div>