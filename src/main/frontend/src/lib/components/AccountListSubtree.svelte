<script lang="ts">
    import Self from './AccountListSubtree.svelte';	
    import { goto } from '$app/navigation';
    const { children, name } = $props();
</script>

<li>
    <h2 class="menu-title text-neutral">{name}</h2>
    <ul>
        {#each children as child}
            {#if Object.hasOwn(child, "active")}
                <li><button onclick={() => goto("/app/accounts/transactions/" + child.id)}>{child.name.split(":").pop()}</button></li>
            {:else}
                <Self {...child}/>
            {/if}
        {/each}
    </ul>
</li>