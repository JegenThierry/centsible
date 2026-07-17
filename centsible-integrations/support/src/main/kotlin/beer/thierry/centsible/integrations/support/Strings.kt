package beer.thierry.centsible.integrations.support

fun firstNonBlank(vararg candidates: String?): String? =
    candidates.firstOrNull { !it.isNullOrBlank() }

fun requireNonBlank(value: String, name: String) {
    require(value.isNotBlank()) { "$name must not be blank" }
}
