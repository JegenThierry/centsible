package beer.thierry.budgetplanner.api.model.recurring

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class RecurringTransactionForm(
    @field:NotNull(message = "Amount is required.")
    @field:DecimalMin(value = "0.01", message = "Amount must be greater than 0.")
    @field:DecimalMax(value = "9999999.99", message = "Amount must not exceed 9,999,999.99.")
    @field:Digits(integer = 7, fraction = 2, message = "Amount must have at most 2 decimal places.")
    var amount: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "Category is required.")
    @field:Positive(message = "Category id must be positive.")
    var categoryId: Long = 0L,

    @field:NotBlank(message = "Description is required.")
    @field:Size(min = 1, max = 255, message = "Description must be 1-255 characters.")
    var description: String = "",

    @field:NotNull(message = "Frequency is required.")
    var frequency: Frequency = Frequency.MONTHLY,

    @field:NotNull(message = "Start date is required.")
    var startDate: LocalDate = LocalDate.now(),

    var endDate: LocalDate? = null,

    var active: Boolean = true,
)
