package beer.thierry.centsible.jooq.repository

import beer.thierry.centsible.api.model.currency.ConversionResult
import java.math.BigDecimal
import java.time.LocalDate

internal data class FxColumns(
    val originalAmount: BigDecimal?,
    val originalCurrency: String?,
    val rate: BigDecimal?,
    val rateDate: LocalDate?,
) {
    companion object {
        fun from(conversion: ConversionResult): FxColumns =
            if (conversion.sameCurrency) {
                FxColumns(null, null, null, null)
            } else {
                FxColumns(
                    originalAmount = conversion.originalAmount,
                    originalCurrency = conversion.originalCurrency.name,
                    rate = conversion.rate,
                    rateDate = conversion.rateDate,
                )
            }
    }
}
