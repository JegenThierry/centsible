package beer.thierry.budgetplannerrest.model.transaction

import java.math.BigDecimal
import java.time.LocalDate

data class TransactionForm(
    val amount: BigDecimal,
    val categoryId: Long,
    val description: String,
    val transactionDate: LocalDate,
)
