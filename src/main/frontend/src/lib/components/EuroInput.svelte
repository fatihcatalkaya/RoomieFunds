<script lang="ts">
	import { NumericFormat, NumberFormatStyle } from 'svelte-number-format';

	interface Props {
		// Value in cents (backend unit). Two-way bound. May be negative when allowNegative is set.
		value?: number | null;
		// Opt-in: let the user type a leading minus. Off for fields where a
		// negative amount is meaningless, such as a product price.
		allowNegative?: boolean;
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
	let {
		value = $bindable(),
		allowNegative = false,
		class: className = '',
		...rest
	}: Props = $props();

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

	// Set when '-' is pressed on an empty field. intl-number-input cannot hold a
	// lone minus, so the sign waits here until the first digit arrives.
	let pendingNegative = false;

	// The rendered input, captured when a sign key is pressed. Used to flip the
	// sign in the field's own text, which is the only way to change it without
	// making NumericFormat re-create the input mid-edit.
	let inputEl: HTMLInputElement | null = null;

	$effect(() => {
		if (value === syncedCents) return;
		syncedCents = value;
		pendingNegative = false;
		euros = toEuros(value);
	});

	// Writes cents outwards and refreshes what the field displays. Used only when
	// we change the sign ourselves — the plain publish() path deliberately leaves
	// the field alone so that typing is never interrupted.
	function pushDown(cents: number | null) {
		syncedCents = cents;
		value = cents;
		euros = toEuros(cents);
	}

	// Keeps the bound cents current on every keystroke, so submitting without
	// leaving the field still sends what the user sees.
	function publish(raw: number | null) {
		const cents = raw == null ? null : Math.round(raw * 100);
		if (pendingNegative && cents != null && cents !== 0) {
			pendingNegative = false;
			// A digit exists now, so the library will accept a minus in the text —
			// which makes the sign visible straight away rather than only on blur.
			// Re-entrancy is bounded: the dispatch calls this function once more,
			// by which time pendingNegative is already cleared.
			if (inputEl && !inputEl.value.includes('-')) {
				inputEl.value = `-${inputEl.value}`;
				inputEl.dispatchEvent(new Event('input', { bubbles: true }));
				return;
			}
			// No element to write through: fall back to reporting the sign on the
			// value alone, and let onfocusout refresh the field afterwards.
			pendingNegative = true;
			const negated = -Math.abs(cents);
			syncedCents = negated;
			value = negated;
			return;
		}

		syncedCents = cents;
		value = cents;
	}

	// Once the user leaves the field, show the sign that publish() has been
	// applying to the bound value all along.
	//
	// focusout rather than onChange: NumericFormat's onChange does not fire
	// reliably on blur. focusout is also dispatched after blur, so this runs
	// after NumericFormat's own blur handler has written its parsed (positive)
	// value into `euros`, and the refresh below is not overwritten again.
	function onfocusout() {
		if (!pendingNegative) return;
		pendingNegative = false;
		if (value == null) return;
		pushDown(value);
	}

	function onkeydown(event: KeyboardEvent) {
		if (!allowNegative) return;
		if (event.key !== '-' && event.key !== '+') return;
		event.preventDefault();

		inputEl = event.currentTarget as HTMLInputElement;

		const minus = event.key === '-';
		if (value == null) {
			// Nothing to flip yet — remember the sign for the first digit.
			pendingNegative = minus ? !pendingNegative : false;
			return;
		}

		// The field already holds digits, so flip the sign in its own text and let
		// intl-number-input re-conform it. Going through the value prop instead
		// would not refresh a field whose euro value happens to be unchanged, and
		// remounting the input would steal focus mid-edit. The library only
		// rejects a minus when there are no integer digits, which is not the case
		// here.
		//
		// The sign now lives in the text, so a minus still pending from an earlier
		// keypress must not negate the result a second time.
		pendingNegative = false;

		const unsigned = inputEl.value.replace('-', '');
		inputEl.value = minus && value >= 0 ? `-${unsigned}` : unsigned;
		inputEl.dispatchEvent(new Event('input', { bubbles: true }));
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
	{onkeydown}
	{onfocusout}
/>
