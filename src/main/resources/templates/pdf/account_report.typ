#set page(paper: "a4")
#set heading(numbering: "1.")
#show heading: it => block(spacing: (1em), it)

#let assets = 810.20
#let floorAccountBalance = 810.20
#let difference = assets - floorAccountBalance

todo this currently does not work

#align(center)[
  #text(weight: "bold", size: 22pt)[Account Report]
  #linebreak()
  Exported from Roomie Funds
]

= Balance of Active Accounts

#table(
  columns: 2,
  table.header[Acount Name][Balance],
  [Bankkonto], align(right)[810,55 €],
  [Barkasse], align(right)[0,00 €],
  [PayPal], align(right)[0,00 €],
)

= Balances of External People

#table(
  columns: 4,
  table.header[Room][Name][Last Payment][Balance],
  [R401], [Peter Lustig], [14.10.2025], [-41,12 €]
)

= Avaiable Funds vs. Open Debts of Former Tenants

#grid(
  columns: (5cm, 1fr),
  table(
    columns: 2,
    [Assets], [#assets €],
    [Floor Account], [#floorAccountBalance €],
    text(weight: "bold")[Difference], [#difference €]
  ),
  [
    Smaller difference (even negative) is better. A large positive difference means there are more outstanding debts than credits.
  ]
)

= Available Funds


