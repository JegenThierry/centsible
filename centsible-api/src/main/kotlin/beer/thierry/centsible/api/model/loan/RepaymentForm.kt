package beer.thierry.centsible.api.model.loan

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

    @field:NotNull(message = "{validation.amount.required}")
    @field:DecimalMin(value = "0.01", message = "{validation.amount.tooSmall}")
    @field:DecimalMax(value = "9999999.99", message = "{validation.amount.tooLarge}")
    @field:Digits(integer = 7, fraction = 2, message = "{validation.amount.fraction}")
    var amount: BigDecimal = BigDecimal.ZERO,

    @field:Size(max = 255, message = "{validation.description.tooLong}")
    var description: String? = null,

    @field:NotNull(message = "{validation.repayment.date.required}")
    var repaidAt: LocalDate = LocalDate.now(),
)
