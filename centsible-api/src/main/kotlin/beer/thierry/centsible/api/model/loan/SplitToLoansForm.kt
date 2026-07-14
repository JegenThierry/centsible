package beer.thierry.centsible.api.model.loan

import beer.thierry.centsible.api.model.budgetaccount.Currency
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Carves portions of an existing expense transaction into tracking-only IOUs (money owed back to
 * you). Each [IouShareForm] becomes a loan linked to the source transaction. The leftover — the
 * transaction amount minus the sum of shares — is treated as the user's own share and stays an
 * ordinary expense.
 */
data class SplitToLoansForm(
    /** Display currency of the source transaction; falls back to the user's default when null. */
    var currency: Currency? = null,

    /** Optional label for the created IOUs; defaults to the source transaction's description. */
    @field:Size(max = 255, message = "{validation.description.tooLong}")
    var description: String? = null,

    @field:Valid
    @field:NotEmpty(message = "{validation.loan.split.empty}")
    var shares: List<IouShareForm> = emptyList(),
)

/** One person's share of a split expense. Provide an existing contact or a new contact's name. */
data class IouShareForm(
    var contactId: UUID? = null,

    @field:Size(max = 100, message = "{validation.firstName.tooLong}")
    var newContactFirstName: String? = null,

    @field:Size(max = 100, message = "{validation.lastName.tooLong}")
    var newContactLastName: String? = null,

    @field:NotNull(message = "{validation.loan.lent.required}")
    @field:DecimalMin(value = "0.01", message = "{validation.loan.lent.tooSmall}")
    @field:DecimalMax(value = "9999999.99", message = "{validation.loan.lent.tooLarge}")
    @field:Digits(integer = 7, fraction = 2, message = "{validation.loan.lent.fraction}")
    var amount: BigDecimal = BigDecimal.ZERO,

    var dueDate: LocalDate? = null,

    @field:Size(max = 255, message = "{validation.description.tooLong}")
    var note: String? = null,
)
