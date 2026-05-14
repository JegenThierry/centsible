package beer.thierry.budgetplanner.api.model.loan

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

    @field:Size(max = 100, message = "First name must be at most 100 characters.")
    var newContactFirstName: String? = null,

    @field:Size(max = 100, message = "Last name must be at most 100 characters.")
    var newContactLastName: String? = null,

    var accountId: UUID? = null,

    var affectBalance: Boolean = true,

    @field:NotNull(message = "Lent amount is required.")
    @field:DecimalMin(value = "0.01", message = "Lent amount must be greater than 0.")
    @field:DecimalMax(value = "9999999.99", message = "Lent amount must not exceed 9,999,999.99.")
    @field:Digits(integer = 7, fraction = 2, message = "Lent amount must have at most 2 decimal places.")
    var lentAmount: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "Owed amount is required.")
    @field:DecimalMin(value = "0.00", message = "Owed amount must be 0 or greater.")
    @field:DecimalMax(value = "9999999.99", message = "Owed amount must not exceed 9,999,999.99.")
    @field:Digits(integer = 7, fraction = 2, message = "Owed amount must have at most 2 decimal places.")
    var owedAmount: BigDecimal = BigDecimal.ZERO,

    @field:NotBlank(message = "Description is required.")
    @field:Size(min = 1, max = 255, message = "Description must be 1-255 characters.")
    var description: String = "",

    @field:NotNull(message = "Transaction date is required.")
    var transactionDate: LocalDate = LocalDate.now(),

    var dueDate: LocalDate? = null,
    var notes: String? = null,
)
