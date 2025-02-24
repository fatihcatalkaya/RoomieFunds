<script lang="ts">
	import { goto } from "$app/navigation";
	import { page } from "$app/state";
	import { deleteApiProductByProductId, patchApiProductByProductId, type Product } from "$lib/client";
    import MdiDelete from "~icons/mdi/delete";

    let { data } = $props();

    let product = data.data as Product;
    let error = data.error;

    let name = $state(product.name);
    let price = $state(product.price);
    let print = $state(product.print);

    async function updateProduct() {
        let query = await patchApiProductByProductId({
            path: {
                productId: product.id!
            },
            body: { name, price, print },
        });

        if (query.error) {
            console.error(query.error)
        } else {
            goto("../")
        }

        return false
    }
    
    let modalElement: HTMLDialogElement;

    async function deleteProduct() {
        modalElement!.showModal();
    }

    async function reallyDeleteProduct() {
        let query = await deleteApiProductByProductId({
            path: {
                productId: product.id!,
            }
        });

        if (query.error) {
            console.error(query.error)
        } else {
            goto("../")
        }

        return true
    }
</script>

<dialog class="modal" bind:this={modalElement}>
    <div class="modal-box">
      <h3 class="text-lg font-bold">{name} löschen</h3>
      <p class="py-4">Bist du dir sicher, dass du Produkt {product.id} löschen willst?</p>
      <div class="modal-action">
        <form method="dialog" class="join">
            <button class="btn btn-error join-item" onclick={reallyDeleteProduct}>Löschen</button>
            <button class="btn join-item">Abbrechen</button>
        </form>
      </div>
    </div>
    <form method="dialog" class="modal-backdrop">
      <button>close</button>
    </form>
  </dialog>

{#if data}
    <div class="inline-flex items-center w-full my-4">
        <h1 class="flex-grow text-2xl font-bold">
            Produkt {page.params.id} Bearbeiten
        </h1>
        <button class="btn btn-error h-8 w-8 p-0 m-0 text-lg" onclick={deleteProduct}>
            <MdiDelete/>
        </button>
    </div>

    <form method="dialog" class="grid grid-cols-1 gap-2 max-w-md mx-auto">
        <label class="w-full flex items-center">
            <span class="w-1/4">ID</span>
            <input type="text" class="input w-3/4" value="{product.id}" disabled />
        </label>
        <label class="w-full flex items-center">
            <span class="w-1/4">Name</span>
            <input type="text" class="input w-3/4" bind:value={name} />
        </label>
        <label class="w-full flex items-center">
            <span class="w-1/4">Price</span>
            <input type="number" class="input w-3/4" bind:value={price} />
        </label>
        <div class="w-full flex items-center">
            <span class="w-1/4 text-center">
            </span>
            <label class="w-3/4 flex items-center gap-2">
                <input type="checkbox" class="checkbox" bind:checked={print}>
                <span class="flex-grow">auf Strichliste drucken</span>
            </label>
        </div>
        <div class="join w-full mt-2">
            <a href="/app/products" class="join-item btn btn-warn w-1/2">
                Zurück
            </a>
            <input class="join-item btn btn-success w-1/2" value="Speichern" type="submit" onclick={updateProduct}>
        </div>
    </form>
{:else if error}
    Error while fetching product!
    <pre class="mt-4">{JSON.stringify(error, null, 2)}</pre>
{/if}

