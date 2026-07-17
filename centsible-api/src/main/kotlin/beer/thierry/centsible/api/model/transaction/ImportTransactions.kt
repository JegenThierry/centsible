package beer.thierry.centsible.api.model.transaction

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryType
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class ImportTransactionRow(
    @field:NotNull(message = "{validation.amount.required}")
    @field:DecimalMin(value = "0.01", message = "{validation.amount.tooSmall}")
    @field:DecimalMax(value = "999999999999.99", message = "{validation.amount.tooLarge}")
    @field:Digits(integer = 12, fraction = 2, message = "{validation.amount.fraction}")
    var amount: BigDecimal = BigDecimal.ZERO,

    @field:NotNull(message = "{validation.category.required}")
    @field:Positive(message = "{validation.category.positive}")
    var categoryId: Long = 0L,

    @field:NotBlank(message = "{validation.description.required}")
    @field:Size(min = 1, max = 255, message = "{validation.description.range}")
    var description: String = "",

    @field:NotNull(message = "{validation.transactionDate.required}")
    var transactionDate: LocalDate = LocalDate.now(),

    var type: CategoryType? = null,

    var currency: Currency? = null,
)

data class ImportTransactionsRequest(
    @field:NotEmpty(message = "{validation.import.rows.required}")
    @field:Size(max = 1000, message = "{validation.import.rows.tooMany}")
    @field:Valid
    var rows: List<ImportTransactionRow> = emptyList(),
)

data class ImportResult(
    var imported: Int = 0,
    var skippedDuplicates: Int = 0,
)
