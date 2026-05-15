package beer.thierry.budgetplanner.api.model.budget

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class BudgetForm(
    @field:NotNull(message = "Category is required.")
    @field:Positive(message = "Category id must be positive.")
    var categoryId: Long = 0L,

    @field:NotNull(message = "Limit is required.")
    @field:DecimalMin(value = "0.01", message = "Limit must be greater than 0.")
    @field:DecimalMax(value = "9999999.99", message = "Limit must not exceed 9,999,999.99.")
    @field:Digits(integer = 7, fraction = 2, message = "Limit must have at most 2 decimal places.")
    var amountLimit: BigDecimal = BigDecimal.ZERO,
)
