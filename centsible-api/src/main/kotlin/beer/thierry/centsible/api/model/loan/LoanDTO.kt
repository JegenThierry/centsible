package beer.thierry.centsible.api.model.loan

import beer.thierry.centsible.api.model.contact.ContactDTO
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class LoanDTO(
    var id: UUID? = null,
    var contact: ContactDTO = ContactDTO(),
    var transaction: TransactionDTO? = null,
    var accountId: UUID? = null,
    var affectsBalance: Boolean = true,
    var lentAmount: BigDecimal = BigDecimal.ZERO,
    var owedAmount: BigDecimal = BigDecimal.ZERO,
    var totalRepaid: BigDecimal = BigDecimal.ZERO,
    var outstanding: BigDecimal = BigDecimal.ZERO,
    var currency: String = "EUR",
    var interestRate: BigDecimal? = null,
    var loanDate: LocalDate? = null,
    var description: String? = null,
    var dueDate: LocalDate? = null,
    var notes: String? = null,
    var createdAt: OffsetDateTime? = null,
    var modifiedAt: OffsetDateTime? = null,
)
