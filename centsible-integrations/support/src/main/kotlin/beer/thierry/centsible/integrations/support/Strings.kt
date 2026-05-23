package beer.thierry.centsible.integrations.support

/**
 * Returns the first non-null, non-blank value from [candidates], or null if none.
 * Used by integration modules when mapping a remote provider's many optional description /
 * counterparty fields to a single displayed value.
 */
fun firstNonBlank(vararg candidates: String?): String? =
    candidates.firstOrNull { !it.isNullOrBlank() }
