package beer.thierry.centsible.integrations.support

/**
 * Returns the first non-null, non-blank value from [candidates], or null if none.
 * Used by integration modules when mapping a remote provider's many optional description /
 * counterparty fields to a single displayed value.
 */
fun firstNonBlank(vararg candidates: String?): String? =
    candidates.firstOrNull { !it.isNullOrBlank() }

/**
 * Throws [IllegalArgumentException] with `"$name must not be blank"` when [value] is blank.
 * Standardised across integration modules so error messages and argument-validation style stay
 * consistent between providers (GoCardless, PayPal, ...).
 */
fun requireNonBlank(value: String, name: String) {
    require(value.isNotBlank()) { "$name must not be blank" }
}
