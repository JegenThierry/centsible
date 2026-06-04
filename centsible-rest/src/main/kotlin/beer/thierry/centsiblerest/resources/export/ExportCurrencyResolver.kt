package beer.thierry.centsiblerest.resources.export

import beer.thierry.centsible.api.model.budgetaccount.Currency

fun primaryCurrencyOf(accountCurrencies: List<Currency>): String =
    accountCurrencies
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<Currency, Int>> { it.value }.thenBy { it.key.ordinal })
        .firstOrNull()?.key?.name
        ?: Currency.EUR.name
