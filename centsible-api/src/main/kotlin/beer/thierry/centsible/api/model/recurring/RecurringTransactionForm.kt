package beer.thierry.centsible.api.model.recurring

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryType
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

data class RecurringTransactionForm(
    @field:NotNull(message = "{validation.amount.required}")
    @field:DecimalMin(value = "0.01", message = "{validation.amount.tooSmall}")
    @field:DecimalMax(value = "999999999999.99", message = "{validation.amount.tooLarge}")
    @field:Digits(integer = 12, fraction = 2, message = "{validation.amount.fraction}")
    var amount: BigDecimal = BigDecimal.ZERO,

    var categoryId: Long? = null,

    @field:NotBlank(message = "{validation.description.required}")
    @field:Size(min = 1, max = 255, message = "{validation.description.range}")
    var description: String = "",

    @field:NotNull(message = "{validation.recurring.frequency.required}")
    var frequency: Frequency = Frequency.MONTHLY,

    @field:NotNull(message = "{validation.recurring.startDate.required}")
    var startDate: LocalDate = LocalDate.now(),

    var endDate: LocalDate? = null,
    var active: Boolean = true,
    var currency: Currency? = null,
    var type: CategoryType? = null,
    var isTransfer: Boolean = false,
    var destinationAccountId: UUID? = null,
)
