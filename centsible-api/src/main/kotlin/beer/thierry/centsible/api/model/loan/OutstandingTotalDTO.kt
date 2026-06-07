package beer.thierry.centsible.api.model.loan

import java.math.BigDecimal

/**
 * Total open loan balance converted into the user's default currency, plus how many loans were left
 * out because an exchange rate was unavailable — so the UI can flag the figure as approximate.
 */
data class OutstandingTotalDTO(
    val outstanding: BigDecimal,
    val excludedCount: Int,
)
