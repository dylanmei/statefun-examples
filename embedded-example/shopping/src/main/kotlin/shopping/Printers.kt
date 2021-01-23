package shopping

import shopping.protocols.generated.Supply
import org.apache.flink.statefun.flink.harness.io.SerializableConsumer

import de.vandermeer.asciitable.*
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment

class SupplyChangedPrinter : SerializableConsumer<Supply.Changed> {
    override fun accept(event: Supply.Changed) {
        newTable().run {
            addRule()
            newHeader(this)
            addRule()
            newRow(this, event)
            addRule()
            println(render(100))
        }
    }

    fun newTable() = AsciiTable().apply {
        val cwc = CWC_FixedWidth()
            .add(5)
            .add(20)
            .add(8)
            .add(8)
        renderer.cwc = cwc
    }

    fun newHeader(table: AsciiTable) = table.addRow(
        null,
        "SUPPLY",
        "Qty",
        "Diff",
    ).apply {
        cells[2].context.textAlignment = TextAlignment.CENTER
        cells[3].context.textAlignment = TextAlignment.CENTER
    }

    fun newRow(table: AsciiTable, event: Supply.Changed) = table.addRow(
        icons[event.id],
        event.id,
        event.totalQuantity,
        formatNumberWithSymbol(event.difference)
    ).apply {
        cells[0].context.textAlignment = TextAlignment.CENTER
        cells[2].context.textAlignment = TextAlignment.RIGHT
        cells[3].context.textAlignment = TextAlignment.RIGHT
    }

    fun formatNumberWithSymbol(n: Int): String = when {
           n > 0 -> "+$n"
           else  -> n.toString()
        }

    companion object {
        private const val serialVersionUID: Long = 1L
        private val icons = mapOf(
            "Shoes" to "\uD83D\uDC5E",
            "Coat" to "\uD83E\uDDE5",
            "Backpack" to "\uD83C\uDF92",
            "Purse" to "\uD83D\uDC5B",
            "Hat" to "\uD83C\uDFA9",
            "Watch" to "\u231A\uFE0F",
            "Pants" to "\uD83D\uDC56",
            "Umbrella" to "\uD83C\uDF02",
            "Chocolate" to "\uD83C\uDF6B",
        )
    }
}
