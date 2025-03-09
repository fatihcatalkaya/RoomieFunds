#set page("a4", margin: 1cm)
#set document(title: [Kontoauszug für Konto {accountName}], date: auto)
#set text(size: 12pt)
#set page(
  numbering: "1",
  footer: context [
    #grid(
      columns: (1fr, 1fr),
      [
        Made with #emoji.heart by RoomieFunds
      ],
      [
        #align(right)[#counter(page).display("1/1", both: true)]
      ],
    )
  ],
)

#align(center, [
  #set text(size: 15pt)
  *Kontoauszug für Konto {accountName}*\
  Auszug vom #datetime.today().display("[day padding:zero].[month padding:zero].[year padding:zero]")
])

#table(
  columns: (1fr, 1fr, 1fr, 1fr, 1fr),
  align: horizon,
  fill: (_, y) => if calc.even(y) and y > 1 \{ rgb("#dededf") \},
  table.header(
    [*Buchungsdatum*], [*Beschreibung*], [*Gebucht gegen*], [#align(right, [*Betrag*])], [#align(right, [*Saldo*])]
  ),
  {#for tx in transactions}
  [{tx.date}], [{tx.description}], [{tx.bookingTarget}], [#align(right,"{tx.amount}")], [
    {#if tx.isBalanceNegative}
    #align(right, text(red, weight: "bold", "{tx.balance}"))
    {#else}
    #align(right, "{tx.balance}")
    {/if}
  ],
  {/for}
)
