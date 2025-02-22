const currencyFormatter = new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" })

/**
 * Format a number as euros
 * 
 * @param number Euro amount in units of cents
 * @returns German-style formatted euros
 */
export const formatEuroCents = (number: number) => {
    return currencyFormatter.format(number / 100)
}