package beer.thierry.centsible.api.model.reports

import beer.thierry.centsible.api.model.category.CategoryType
import java.math.BigDecimal
import java.time.LocalDate

/**
 * A single projected recurring occurrence within the forecast horizon, already converted to the
 * user's default currency. [amount] is a positive magnitude; direction is carried by [type].
 */
data class ForecastOccurrenceDTO(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val type: CategoryType,
    val categoryName: String?,
    val categoryColor: String?,
)
