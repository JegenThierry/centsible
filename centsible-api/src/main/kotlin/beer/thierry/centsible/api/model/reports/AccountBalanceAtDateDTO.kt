package beer.thierry.centsible.api.model.reports

import beer.thierry.centsible.api.model.budgetaccount.AccountType
import beer.thierry.centsible.api.model.budgetaccount.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class AccountBalanceAtDateDTO(
    val accountId: UUID,
    val accountName: String,
    val currency: Currency,
    val type: AccountType,
    val balance: BigDecimal,
    val convertedBalance: BigDecimal?,
    val targetCurrency: Currency,
    val date: LocalDate,
)
