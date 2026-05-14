package beer.thierry.budgetplanner.api.model.budgetaccount

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateBudgetAccountRequest(
    @field:NotBlank(message = "Account name is required.")
    @field:Size(max = 100, message = "Account name must be at most 100 characters.")
    var name: String = "",

    @field:NotNull(message = "Initial balance is required.")
    @field:Digits(integer = 13, fraction = 2, message = "Initial balance must have at most 2 decimal places.")
    var initialBalance: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "Currency is required.")
    var currency: Currency? = Currency.EUR,
)
