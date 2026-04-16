package beer.thierry.budgetplanner.api.model.transaction

import java.math.BigDecimal
import java.time.LocalDate

data class TransactionForm(
    var amount: BigDecimal = BigDecimal.ZERO,
    var categoryId: Long = 0L,
    var description: String = "",
    var transactionDate: LocalDate = LocalDate.now(),
)
