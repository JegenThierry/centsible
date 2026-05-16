package beer.thierry.centsible.api.model.recurring

import beer.thierry.centsible.api.model.category.CategoryDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class RecurringTransactionDTO(
    var id: UUID? = null,
    var accountId: UUID? = null,
    var category: CategoryDTO = CategoryDTO(),
    var amount: BigDecimal? = null,
    var description: String? = null,
    var frequency: Frequency? = null,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    var nextRunAt: LocalDate? = null,
    var active: Boolean = true,
    var createdAt: OffsetDateTime? = null,
    var updatedAt: OffsetDateTime? = null,
)
