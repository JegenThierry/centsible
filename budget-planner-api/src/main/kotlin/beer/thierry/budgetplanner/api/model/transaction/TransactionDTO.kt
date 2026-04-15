package beer.thierry.budgetplanner.api.model.transaction

import java.math.BigDecimal
import java.time.OffsetDateTime
import beer.thierry.budgetplanner.api.model.category.CategoryDTO
import java.time.LocalDate
import java.util.UUID

data class TransactionDTO(
    var id: UUID? = null,
    var category: CategoryDTO = CategoryDTO(),
    var amount: BigDecimal? = null,
    var description: String? = null,
    var transactionDate: LocalDate? = null,
    var createdAt: OffsetDateTime? = null,
    var updatedAt: OffsetDateTime? = null
)
