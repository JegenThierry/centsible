package beer.thierry.budgetplannerrest.model.budgetaccount

import java.math.BigDecimal
import java.time.OffsetDateTime

data class BudgetAccountDTO(
    val id: String,
    val name: String,
    val balance: BigDecimal,
    val initialBalance: BigDecimal,
    val currency: Currency,
)

data class BudgetAccountSnapshotDTO(
    val id: Number,
    val accountId: String,
    val balance: BigDecimal,
    val createdAt: OffsetDateTime,
)
