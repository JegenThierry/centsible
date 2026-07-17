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

/**
 * Returns the value at [key] as a non-blank String, or null when it is absent, not a String, or
 * blank. Replaces the `(config[key] as? String)?.takeIf { it.isNotBlank() }` idiom the provider
 * modules repeat when reading string fields out of a connection's `config` / a request's `values`.
 */
fun Map<String, Any?>.nonBlankString(key: String): String? =
    (this[key] as? String)?.takeIf { it.isNotBlank() }

/**
 * Like [nonBlankString] but throws [IllegalArgumentException] `"$name is required"` when the value
 * is missing, blank, or not a String. [name] is the human-facing field label used in the message.
 */
fun Map<String, Any?>.requiredString(key: String, name: String): String =
    nonBlankString(key) ?: throw IllegalArgumentException("$name is required")
