package beer.thierry.budgetplanner.api.model.budgetaccount

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class BudgetAccountDTO(
    var id: UUID = UUID.randomUUID(),
    var name: String = "",
    var balance: BigDecimal = BigDecimal.ZERO,
    var initialBalance: BigDecimal = BigDecimal.ZERO,
    var currency: Currency = Currency.EUR,
)

data class BudgetAccountSnapshotDTO(
    var id: Number = 0,
    var accountId: UUID = UUID.randomUUID(),
    var balance: BigDecimal = BigDecimal.ZERO,
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
)
