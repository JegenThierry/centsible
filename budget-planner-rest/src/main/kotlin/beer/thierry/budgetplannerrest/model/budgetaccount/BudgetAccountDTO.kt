package beer.thierry.budgetplannerrest.model.budgetaccount

import java.math.BigDecimal

data class BudgetAccountDTO(
    val id: String,
    val name: String,
    val balance: BigDecimal,
    val currency: Currency,
)