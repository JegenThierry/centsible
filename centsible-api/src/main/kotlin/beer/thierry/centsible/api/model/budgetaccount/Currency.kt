package beer.thierry.centsible.api.model.budgetaccount

enum class Currency {
    EUR, USD, JPY, GBP, AUD, CAD, CHF, CNY, HKD, NZD,
    SEK, NOK, DKK, SGD, KRW, INR, MXN, BRL, ZAR, TRY,
    PLN, PHP, IDR;

    companion object {
        /** Leniently parse a currency code (trim + uppercase); null if blank or not a known [Currency]. */
        fun parseOrNull(code: String?): Currency? =
            code?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
                runCatching { valueOf(raw.uppercase()) }.getOrNull()
            }
    }
}
