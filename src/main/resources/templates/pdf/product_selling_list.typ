#set page("a4", margin: 1cm, flipped: true)

#set text(size: 12pt)
#set page(
  numbering: "1",
  footer: context [
    #grid(
      columns: (1fr, 1fr, 1fr),
      [
        Made with #emoji.heart by RoomieFunds
      ],
      [
        #align(center)[#datetime.today().display("[day padding:zero].[month padding:zero].[year padding:zero]")]
      ],
      [
        #align(right)[#counter(page).display("1/1", both: true)]
      ],
    )
  ],
)

#let columnSize = 3cm
#let inset = 0.2cm
#let numColumnsPerPage = 9

#let repeat(elem, n) = {
  let res = ()
  let iter = 0
  while iter < n {
    res.push(elem)
    iter = iter + 1
  }
  return res
}

#align(center)[
  #table(
    columns: repeat(columnSize, 9),
    inset: inset,
    fill: (_, y) => if calc.even(y) and y > 1 { rgb("#dededf") },
    table.header(
      repeat: false,
      table.cell(rowspan: 2)[#align(horizon)[*Zimmer*]],
      [*Cola*],
      [*Fanta*],
      [*Bier*],
      [*Club Mate*],
      [*Spezi*],
      [*ACE*],
      [*Coca Cola*],
      [*Baum*],
      "1,00 €",
      "1,00 €",
      "1,00 €",
      "1,00 €",
      "1,00 €",
      "1,00 €",
      "1,00 €",
      "1,00 €",
    ),
    table.hline(stroke: 1.8pt),
    [R401], [], [], [], [], [], [], [], [],
    [R402], [], [], [], [], [], [], [], [],
    [R403], [], [], [], [], [], [], [], [],
    [R404], [], [], [], [], [], [], [], [],
    [R405], [], [], [], [], [], [], [], [],
    [R406], [], [], [], [], [], [], [], [],
    [R407], [], [], [], [], [], [], [], [],
    [R408], [], [], [], [], [], [], [], [],
    [R409], [], [], [], [], [], [], [], [],
    [R410], [], [], [], [], [], [], [], [],
    [R411], [], [], [], [], [], [], [], [],
    [R412], [], [], [], [], [], [], [], [],
    [R413], [], [], [], [], [], [], [], [],
    [R414], [], [], [], [], [], [], [], [],
  )
]