package beer.thierry.budgetplanner.api.model.budgetaccount

import java.math.BigDecimal

data class CreateBudgetAccountRequest(
    var name: String = "",
    var initialBalance: BigDecimal = BigDecimal.ZERO,
    var currency: Currency? = Currency.EUR,
)