package beer.thierry.centsible.api.model.reports

import beer.thierry.centsible.api.model.category.CategoryType
import java.math.BigDecimal
import java.time.LocalDate

data class ForecastOccurrenceDTO(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val type: CategoryType,
    val categoryName: String?,
    val categoryColor: String?,
)
