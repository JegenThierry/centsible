package beer.thierry.centsible.api.model.reports

import beer.thierry.centsible.api.model.budgetaccount.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class AccountBalanceAtDateDTO(
    val accountId: UUID,
    val accountName: String,
    val currency: Currency,
    val balance: BigDecimal,
    /** [balance] converted into [targetCurrency] (the user's default); null when no FX rate could be resolved. */
    val convertedBalance: BigDecimal?,
    val targetCurrency: Currency,
    val date: LocalDate,
)
