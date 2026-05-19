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
    val date: LocalDate,
)
