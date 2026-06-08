package beer.thierry.centsible.api.model.loan

import beer.thierry.centsible.api.model.transaction.TransactionDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class RepaymentDTO(
    var id: UUID? = null,
    var loanId: UUID? = null,
    var transaction: TransactionDTO? = null,
    /** Whether the repayment is backed by a real account transaction (vs. tracking-only). */
    var affectsBalance: Boolean = true,
    var amount: BigDecimal = BigDecimal.ZERO,
    var currency: String? = null,
    var repaidAt: LocalDate? = null,
    var createdAt: OffsetDateTime? = null,
)
