package beer.thierry.budgetplannerrest.model.transaction

import java.math.BigDecimal
import java.time.OffsetDateTime
import beer.thierry.budgetplannerrest.model.category.CategoryDTO
import java.time.LocalDate
import java.util.UUID

data class TransactionDTO(
    val id: UUID?,
    val category: CategoryDTO,
    val amount: BigDecimal?,
    val description: String?,
    val transactionDate: LocalDate?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?
)
