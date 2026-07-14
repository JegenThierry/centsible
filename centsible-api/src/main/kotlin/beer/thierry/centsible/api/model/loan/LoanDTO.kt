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
    /** For an IOU carved from an expense (see split-to-loans): the source transaction it belongs to. */
    var sourceTransactionId: UUID? = null,
    var accountId: UUID? = null,
    /** Whether the loan is backed by a real account transaction (vs. tracking-only). */
    var affectsBalance: Boolean = true,
    var lentAmount: BigDecimal = BigDecimal.ZERO,
    /** Total expected back, i.e. [lentAmount] plus any interest. */
    var owedAmount: BigDecimal = BigDecimal.ZERO,
    var totalRepaid: BigDecimal = BigDecimal.ZERO,
    /** Still due, i.e. [owedAmount] minus [totalRepaid]. */
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
