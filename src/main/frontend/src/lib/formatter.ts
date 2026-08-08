const currencyFormatter = new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' });
const dateFormatter = new Intl.DateTimeFormat('de-DE', {
	day: '2-digit',
	month: '2-digit',
	year: 'numeric',
	timeZone: 'UTC'
});

/**
 * Format a number as euros
 *
 * @param number Euro amount in units of cents
 * @returns German-style formatted euros
 */
export const formatEuroCents = (number: number) => {
	return currencyFormatter.format(number / 100);
};

/**
 * Format an ISO yyyy-MM-dd date string as a German dd.MM.yyyy date
 *
 * @param isoDate ISO date string (yyyy-MM-dd), or null/undefined/empty
 * @returns German-style formatted date, or '' if isoDate is missing/invalid
 */
export const formatIsoDate = (isoDate: string | null | undefined): string => {
	if (!isoDate) {
		return '';
	}

	// Parse explicitly as UTC and format with timeZone: 'UTC' above so parsing and
	// formatting agree - otherwise a local-time formatter would shift the date back
	// a day for users at negative UTC offsets.
	const d = new Date(`${isoDate}T00:00:00Z`);
	if (Number.isNaN(d.getTime())) {
		return '';
	}

	return dateFormatter.format(d);
};

/**
 * Get today's date in the user's local timezone, formatted as ISO yyyy-MM-dd
 *
 * @returns today's local calendar date as an ISO yyyy-MM-dd string
 */
export const todayAsIsoDate = (): string => {
	// Deliberately NOT new Date().toISOString().substring(0, 10): toISOString()
	// converts to UTC first, which yields the wrong calendar day for users whose
	// local date differs from the current UTC date. Read local fields directly instead.
	const d = new Date();
	const year = d.getFullYear();
	const month = String(d.getMonth() + 1).padStart(2, '0');
	const day = String(d.getDate()).padStart(2, '0');
	return `${year}-${month}-${day}`;
};
