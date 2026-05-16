package beer.thierry.budgetplanner.api.model.budgetaccount

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateBudgetAccountRequest(
    @field:NotBlank(message = "{validation.account.name.required}")
    @field:Size(max = 100, message = "{validation.account.name.tooLong}")
    var name: String = "",

    @field:NotNull(message = "{validation.account.initialBalance.required}")
    @field:Digits(integer = 13, fraction = 2, message = "{validation.account.initialBalance.fraction}")
    var initialBalance: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "{validation.account.currency.required}")
    var currency: Currency? = Currency.EUR,
)
