CREATE TYPE "public"."received_from" AS ENUM('cash', 'bank_account', 'paypal');--> statement-breakpoint
CREATE TABLE "account" (
	"id" bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY (sequence name "account_id_seq" INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START WITH 1 CACHE 1),
	"name" text NOT NULL,
	"active_account" boolean NOT NULL
);
--> statement-breakpoint
CREATE TABLE "person" (
	"id" bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY (sequence name "person_id_seq" INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START WITH 1 CACHE 1),
	"name" text NOT NULL,
	"room" text NOT NULL,
	"pays_floor_fee" boolean NOT NULL,
	"account_id" bigint NOT NULL
);
--> statement-breakpoint
CREATE TABLE "physical_transaction" (
	"id" bigint,
	"source" "received_from" NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"description" text DEFAULT '' NOT NULL
);
--> statement-breakpoint
CREATE TABLE "transaction" (
	"id" bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY (sequence name "transaction_id_seq" INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START WITH 1 CACHE 1),
	"source_account" bigint NOT NULL,
	"amount" integer NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	"description" text DEFAULT '' NOT NULL
);
--> statement-breakpoint
ALTER TABLE "person" ADD CONSTRAINT "person_account_id_account_id_fk" FOREIGN KEY ("account_id") REFERENCES "public"."account"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "physical_transaction" ADD CONSTRAINT "physical_transaction_id_transaction_id_fk" FOREIGN KEY ("id") REFERENCES "public"."transaction"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "transaction" ADD CONSTRAINT "transaction_source_account_account_id_fk" FOREIGN KEY ("source_account") REFERENCES "public"."account"("id") ON DELETE no action ON UPDATE no action;