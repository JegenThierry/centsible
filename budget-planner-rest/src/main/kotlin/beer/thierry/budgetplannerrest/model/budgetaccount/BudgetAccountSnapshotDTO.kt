package beer.thierry.budgetplannerrest.model.budgetaccount

import beer.thierry.budgetplannerrest.model.transaction.TransactionDTO
import java.math.BigDecimal
import java.time.LocalDate

data class BudgetAccountSnapshotDTO(
    val date: LocalDate,
    val balance: BigDecimal,
    val transactions: List<TransactionDTO>
)
