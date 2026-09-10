import kotlin.math.round

fun main() {
    val rawSubtotal = 1.0 * 100.5
    val subtotal = round(rawSubtotal * 100.0) / 100.0
    val rawTaxAmount = (rawSubtotal * 10.0) / 100.0
    val taxAmount = round(rawTaxAmount * 100.0) / 100.0
    val cgstRaw = taxAmount / 2.0
    val cgstRounded = round(cgstRaw * 100.0) / 100.0
    val sgstRounded = taxAmount - cgstRounded

    println("rawSubtotal $rawSubtotal subtotal $subtotal rawTax $rawTaxAmount taxAmount $taxAmount cgstRaw $cgstRaw cgstRounded $cgstRounded sgstRounded $sgstRounded")
}
