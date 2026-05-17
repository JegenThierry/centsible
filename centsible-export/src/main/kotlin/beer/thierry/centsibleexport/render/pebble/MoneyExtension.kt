package beer.thierry.centsibleexport.render.pebble

import io.pebbletemplates.pebble.extension.AbstractExtension
import io.pebbletemplates.pebble.extension.Filter
import io.pebbletemplates.pebble.template.EvaluationContext
import io.pebbletemplates.pebble.template.PebbleTemplate
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class MoneyFilter : Filter {
    override fun apply(
        input: Any?,
        args: MutableMap<String, Any?>,
        self: PebbleTemplate?,
        context: EvaluationContext?,
        lineNumber: Int,
    ): Any {
        if (input == null) return ""
        val amount: BigDecimal = when (input) {
            is BigDecimal -> input
            is Number -> BigDecimal(input.toString())
            is String -> input.toBigDecimalOrNull() ?: return input
            else -> return input.toString()
        }
        val currencyCode = (args["currency"] as? String)?.takeIf { it.isNotBlank() } ?: "EUR"
        val localeTag = (args["locale"] as? String)?.takeIf { it.isNotBlank() } ?: "en-GB"
        val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(localeTag)).apply {
            currency = Currency.getInstance(currencyCode)
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return formatter.format(amount)
    }

    override fun getArgumentNames(): List<String> = listOf("currency", "locale")
}

class MoneyExtension : AbstractExtension() {
    override fun getFilters(): Map<String, Filter> = mapOf("money" to MoneyFilter())
}
