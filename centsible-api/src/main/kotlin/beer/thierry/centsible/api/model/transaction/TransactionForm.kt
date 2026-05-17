package beer.thierry.centsible.api.model.transaction

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class TransactionForm(
    @field:NotNull(message = "{validation.amount.required}")
    @field:DecimalMin(value = "0.01", message = "{validation.amount.tooSmall}")
    @field:DecimalMax(value = "9999999.99", message = "{validation.amount.tooLarge}")
    @field:Digits(integer = 7, fraction = 2, message = "{validation.amount.fraction}")
    var amount: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "{validation.category.required}")
    @field:Positive(message = "{validation.category.positive}")
    var categoryId: Long = 0L,

    @field:NotBlank(message = "{validation.description.required}")
    @field:Size(min = 1, max = 255, message = "{validation.description.range}")
    var description: String = "",

    @field:NotNull(message = "{validation.transactionDate.required}")
    var transactionDate: LocalDate = LocalDate.now(),
)
