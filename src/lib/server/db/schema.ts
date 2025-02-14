import { pgTable, bigint, text, boolean, integer, timestamp, pgEnum } from "drizzle-orm/pg-core";

export const Account = pgTable('account', {
    id: bigint('id', { mode: 'bigint' }).primaryKey().generatedAlwaysAsIdentity(),
    name: text('name').notNull(),
    activeAccount: boolean('active_account').notNull()
})

export const Person = pgTable('person', {
    id: bigint('id', { mode: 'bigint' }).primaryKey().generatedAlwaysAsIdentity(),
    name: text('name').notNull(),
    room: text('room').notNull(),
    paysFloorFee: boolean('pays_floor_fee').notNull(),
    accountId: bigint('account_id', { mode: 'bigint' }).notNull().references(() => Account.id)
});

export const Transaction = pgTable('transaction', {
    id: bigint('id', { mode: 'bigint' }).primaryKey().generatedAlwaysAsIdentity(),
    sourceAccount: bigint('source_account', { mode: 'bigint' }).notNull().references(() => Account.id),
    targetAccount: bigint('source_account', { mode: 'bigint' }).notNull().references(() => Account.id),
    amount: integer('amount').notNull(),
    createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
    description: text('description').notNull().default('')
})

export const ReceivedFrom = pgEnum('received_from', ['cash', 'bank_account', 'paypal']);

export const PhysicalTransaction = pgTable('physical_transaction', {
    id: bigint('id', { mode: 'bigint' }).primaryKey().generatedAlwaysAsIdentity(),
    transactionId: bigint('id', { mode: 'bigint' }).references(() => Transaction.id),
    source: ReceivedFrom().notNull(),
    createdAt: timestamp('created_at', { withTimezone: true }).defaultNow().notNull(),
    description: text('description').notNull().default('')
})