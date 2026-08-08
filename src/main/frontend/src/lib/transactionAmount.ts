import type { Transaction } from '$lib/client';

/** Just the fields of an account that affect booking direction. */
export type AccountSide = {
	id?: number;
	active?: boolean;
};

export type Booking = {
	amount: number;
	sourceAccountId: number;
	targetAccountId: number;
};

/**
 * How many cents this transaction moves the given account's saldo by.
 * Negative means the account goes down.
 *
 * Mirrors TransactionService.getTransactionsForAccount: for a mixed
 * Aktiv/Passiv pair the stored amount applies as-is to both accounts, so its
 * own sign carries the direction. For a same-type pair the direction comes
 * from the ordering instead, and the source account is the one that goes down.
 */
export function signedCentsFor(transaction: Transaction, openedAccountId: number): number {
	const amount = transaction.amount ?? 0;

	if (transaction.sourceAccountActive !== transaction.targetAccountActive) {
		return amount;
	}

	return transaction.sourceAccountId === openedAccountId ? -amount : amount;
}

/**
 * The inverse of signedCentsFor: what to send so that the opened account's
 * saldo moves by exactly signedCents.
 *
 * The counter account then follows the existing bookkeeping rules on its own —
 * it moves the opposite way for a same-type pair, and the same way for a mixed
 * Aktiv/Passiv pair, because a balance sheet grows and shrinks on both sides.
 */
export function bookingFor(
	signedCents: number,
	opened: AccountSide,
	counter: AccountSide
): Booking {
	if (opened.active !== counter.active) {
		return {
			amount: signedCents,
			sourceAccountId: opened.id!,
			targetAccountId: counter.id!
		};
	}

	return signedCents < 0
		? { amount: -signedCents, sourceAccountId: opened.id!, targetAccountId: counter.id! }
		: { amount: signedCents, sourceAccountId: counter.id!, targetAccountId: opened.id! };
}
