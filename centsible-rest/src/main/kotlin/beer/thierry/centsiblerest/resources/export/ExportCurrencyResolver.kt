package beer.thierry.centsiblerest.resources.export

import beer.thierry.centsible.api.model.budgetaccount.Currency

/** Picks the most frequent currency (ties broken by lowest [Currency] ordinal), defaulting to [Currency.EUR] when empty. */
fun primaryCurrencyOf(accountCurrencies: List<Currency>): String =
    accountCurrencies
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<Currency, Int>> { it.value }.thenBy { it.key.ordinal })
        .firstOrNull()?.key?.name
        ?: Currency.EUR.name
