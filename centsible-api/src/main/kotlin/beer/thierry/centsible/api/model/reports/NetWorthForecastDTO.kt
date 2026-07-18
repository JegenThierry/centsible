package beer.thierry.centsible.api.model.reports

import beer.thierry.centsible.api.model.budgetaccount.Currency

data class NetWorthForecastDTO(
    val currency: Currency,
    val points: List<NetWorthPointDTO>,
    val occurrences: List<ForecastOccurrenceDTO>,
)
