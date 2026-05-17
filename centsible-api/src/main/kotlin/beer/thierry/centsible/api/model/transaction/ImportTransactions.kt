package beer.thierry.centsible.api.model.transaction

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
    @field:NotNull
    @field:DecimalMin(value = "0.01")
    @field:DecimalMax(value = "9999999.99")
    @field:Digits(integer = 7, fraction = 2)
    var amount: BigDecimal = BigDecimal.ZERO,

    @field:NotNull
    @field:Positive
    var categoryId: Long = 0L,

    @field:NotBlank
    @field:Size(min = 1, max = 255)
    var description: String = "",

    @field:NotNull
    var transactionDate: LocalDate = LocalDate.now(),
)

data class ImportTransactionsRequest(
    @field:NotEmpty
    @field:Size(max = 1000, message = "Cannot import more than 1000 rows at once.")
    @field:Valid
    var rows: List<ImportTransactionRow> = emptyList(),
)

data class ImportResult(
    var imported: Int = 0,
    var skippedDuplicates: Int = 0,
)
