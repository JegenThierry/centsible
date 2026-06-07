package beer.thierry.centsible.api.model.loan

import beer.thierry.centsible.api.model.budgetaccount.Currency
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class LoanForm(
    var contactId: UUID? = null,

    @field:Size(max = 100, message = "{validation.firstName.tooLong}")
    var newContactFirstName: String? = null,

    @field:Size(max = 100, message = "{validation.lastName.tooLong}")
    var newContactLastName: String? = null,

    var accountId: UUID? = null,

    var affectBalance: Boolean = true,

    @field:NotNull(message = "{validation.loan.lent.required}")
    @field:DecimalMin(value = "0.01", message = "{validation.loan.lent.tooSmall}")
    @field:DecimalMax(value = "9999999.99", message = "{validation.loan.lent.tooLarge}")
    @field:Digits(integer = 7, fraction = 2, message = "{validation.loan.lent.fraction}")
    var lentAmount: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "{validation.loan.owed.required}")
    @field:DecimalMin(value = "0.00", message = "{validation.loan.owed.tooSmall}")
    @field:DecimalMax(value = "9999999.99", message = "{validation.loan.owed.tooLarge}")
    @field:Digits(integer = 7, fraction = 2, message = "{validation.loan.owed.fraction}")
    var owedAmount: BigDecimal = BigDecimal.ZERO,

    /** Loan currency. When null the service defaults to the account currency (or the user's default). */
    var currency: Currency? = null,

    /** Optional annual interest as a percentage (e.g. 5.25). When set, owed = lent * (1 + rate/100). */
    @field:DecimalMin(value = "0.0", message = "{validation.loan.interestRate.tooSmall}")
    @field:DecimalMax(value = "999.99", message = "{validation.loan.interestRate.tooLarge}")
    @field:Digits(integer = 3, fraction = 2, message = "{validation.loan.interestRate.fraction}")
    var interestRate: BigDecimal? = null,

    @field:NotBlank(message = "{validation.description.required}")
    @field:Size(min = 1, max = 255, message = "{validation.description.range}")
    var description: String = "",

    @field:NotNull(message = "{validation.transactionDate.required}")
    var transactionDate: LocalDate = LocalDate.now(),

    var dueDate: LocalDate? = null,
    var notes: String? = null,
)
