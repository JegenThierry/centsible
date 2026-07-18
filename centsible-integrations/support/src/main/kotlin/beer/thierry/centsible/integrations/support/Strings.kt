package beer.thierry.centsible.integrations.support

fun firstNonBlank(vararg candidates: String?): String? =
    candidates.firstOrNull { !it.isNullOrBlank() }

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
