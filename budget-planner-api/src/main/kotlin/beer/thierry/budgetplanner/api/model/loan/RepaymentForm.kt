package beer.thierry.budgetplanner.api.model.loan

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class RepaymentForm(
    var accountId: UUID? = null,

    var affectBalance: Boolean = true,

    @field:NotNull(message = "Amount is required.")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0.")
    @field:DecimalMax(value = "9999999.99", message = "Amount must not exceed 9,999,999.99.")
    @field:Digits(integer = 7, fraction = 2, message = "Amount must have at most 2 decimal places.")
    var amount: BigDecimal = BigDecimal.ZERO,

    @field:Size(max = 255, message = "Description must be at most 255 characters.")
    var description: String? = null,

    @field:NotNull(message = "Repayment date is required.")
    var repaidAt: LocalDate = LocalDate.now(),
)
