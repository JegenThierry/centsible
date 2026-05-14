package beer.thierry.budgetplanner.api.model.loan

import beer.thierry.budgetplanner.api.model.transaction.TransactionDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class RepaymentDTO(
    var id: UUID? = null,
    var loanId: UUID? = null,
    var transaction: TransactionDTO? = null,
    var affectsBalance: Boolean = true,
    var amount: BigDecimal = BigDecimal.ZERO,
    var repaidAt: LocalDate? = null,
    var createdAt: OffsetDateTime? = null,
)
