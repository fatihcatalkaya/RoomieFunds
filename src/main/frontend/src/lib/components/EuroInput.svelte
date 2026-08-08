<script lang="ts">
	import { NumericFormat, NumberFormatStyle } from 'svelte-number-format';

	interface Props {
		// Value in cents (backend unit). Two-way bound.
		value?: number | null;
		class?: string;
		id?: string;
		name?: string;
		placeholder?: string;
		required?: boolean;
		disabled?: boolean;
		form?: string;
	}

	// No fallback in $bindable(): the create forms legitimately start out with an
	// unset value (`let price: number | undefined = $state()`), and Svelte rejects
	// `bind:value={undefined}` when the prop declares a fallback.
	let { value = $bindable(), class: className = '', ...rest }: Props = $props();

	function toEuros(cents: number | null | undefined): number | null {
		return cents == null ? null : cents / 100;
	}

	// The euro value handed to NumericFormat. It must only change when `value`
	// changes from the outside: NumericFormat re-creates its underlying
	// NumberInput whenever this prop changes, which rewrites the text and parks
	// the caret at the end. Echoing our own per-keystroke write-back into it
	// froze the field after the first character.
	let euros: number | null = $state(toEuros(value));

	// Cents last exchanged with the outside, so the effect below can tell an
	// external update apart from the write-back we just did ourselves.
	let syncedCents: number | null | undefined = value;

	$effect(() => {
		if (value === syncedCents) return;
		syncedCents = value;
		euros = toEuros(value);
	});

	// Keeps the bound cents current on every keystroke, so submitting without
	// leaving the field still sends what the user sees.
	function publish(raw: number | null) {
		syncedCents = raw == null ? null : Math.round(raw * 100);
		value = syncedCents;
	}
</script>

<!--
	`bind:value` (rather than a one-way prop) lets NumericFormat report the value
	it settled on when the field is left, which keeps `euros` in step with what is
	actually displayed. Without that, re-applying a previously displayed amount
	would not resync the input.
-->
<NumericFormat
	bind:value={euros}
	onInput={publish}
	onChange={publish}
	locale="de-DE"
	options={{
		formatStyle: NumberFormatStyle.Currency,
		currency: 'EUR',
		precision: 2
	}}
	class={className}
	{...rest}
/>
