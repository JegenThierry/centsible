package beer.thierry.centsible.api.model.budget

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class BudgetForm(
    @field:NotNull(message = "{validation.category.required}")
    @field:Positive(message = "{validation.category.positive}")
    var categoryId: Long = 0L,

    @field:NotNull(message = "{validation.budget.limit.required}")
    @field:DecimalMin(value = "0.01", message = "{validation.budget.limit.tooSmall}")
    @field:DecimalMax(value = "9999999.99", message = "{validation.budget.limit.tooLarge}")
    @field:Digits(integer = 7, fraction = 2, message = "{validation.budget.limit.fraction}")
    var amountLimit: BigDecimal = BigDecimal.ZERO,
)
