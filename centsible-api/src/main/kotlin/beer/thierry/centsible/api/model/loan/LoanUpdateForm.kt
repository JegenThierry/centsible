package beer.thierry.centsible.api.model.loan

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Balance-neutral edits to an existing loan. Deliberately excludes lent amount, account, currency and
 * the balance-affecting flag: changing those would require reversing/re-converting the linked
 * transaction, so they stay create-only (delete + recreate). When [interestRate] is set the service
 * recomputes [owedAmount] = lent * (1 + rate/100); otherwise [owedAmount] is taken as provided.
 */
data class LoanUpdateForm(
    @field:NotBlank(message = "{validation.description.required}")
    @field:Size(min = 1, max = 255, message = "{validation.description.range}")
    var description: String = "",

    @field:NotNull(message = "{validation.loan.owed.required}")
    @field:DecimalMin(value = "0.00", message = "{validation.loan.owed.tooSmall}")
    @field:DecimalMax(value = "9999999.99", message = "{validation.loan.owed.tooLarge}")
    @field:Digits(integer = 7, fraction = 2, message = "{validation.loan.owed.fraction}")
    var owedAmount: BigDecimal = BigDecimal.ZERO,

    @field:DecimalMin(value = "0.0", message = "{validation.loan.interestRate.tooSmall}")
    @field:DecimalMax(value = "999.99", message = "{validation.loan.interestRate.tooLarge}")
    @field:Digits(integer = 3, fraction = 2, message = "{validation.loan.interestRate.fraction}")
    var interestRate: BigDecimal? = null,

    var dueDate: LocalDate? = null,
    var notes: String? = null,
)
