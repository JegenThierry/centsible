package beer.thierry.budgetplanner.api.model.transaction

import beer.thierry.budgetplanner.api.model.category.CategoryDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class TransactionDTO(
    var id: UUID? = null,
    var category: CategoryDTO = CategoryDTO(),
    var amount: BigDecimal? = null,
    var description: String? = null,
    var transactionDate: LocalDate? = null,
    var createdAt: OffsetDateTime? = null,
    var updatedAt: OffsetDateTime? = null
)
