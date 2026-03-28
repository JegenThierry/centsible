package beer.thierry.budgetplannerrest.model.budgetaccount

import java.math.BigDecimal

data class CreateBudgetAccountRequest(
    val name: String,
    val initialBalance: BigDecimal,
    val currency: Currency? = Currency.EUR,
)