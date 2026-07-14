package beer.thierry.centsible.api.model.reports

import beer.thierry.centsible.api.model.budgetaccount.Currency

/**
 * Forward-looking net-worth projection: the running balance stepped forward from today's actual net
 * worth by the active, non-transfer recurring rules, plus the upcoming occurrences that drive it.
 * All monetary values are expressed in [currency] (the user's default currency).
 */
data class NetWorthForecastDTO(
    val currency: Currency,
    val points: List<NetWorthPointDTO>,
    val occurrences: List<ForecastOccurrenceDTO>,
)
