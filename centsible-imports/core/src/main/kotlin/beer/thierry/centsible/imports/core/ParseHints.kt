package beer.thierry.centsible.imports.core

import java.util.Locale

/**
 * Per-request parsing context. Every field is optional; parsers fall back to sensible defaults
 * when a hint is absent. CSV-specific hints are ignored by non-CSV parsers.
 */
data class ParseHints(
    /** User locale for date/number parsing fallback. */
    val locale: Locale? = null,
    /** Currency of the target account. Helps parsers normalize signs/amounts. */
    val targetCurrency: String? = null,
    /** Default category id for rows the parser can't categorize on its own. */
    val defaultCategoryId: Long? = null,
    /** CSV: column mapping selected in the wizard (profile-suggested then user-tweaked). */
    val csvMapping: CsvColumnMapping? = null,
    /** CSV: dialect overrides (delimiter, quote, encoding, header presence). */
    val csvDialect: CsvDialect? = null,
)

data class CsvDialect(
    val delimiter: Char = ',',
    val quote: Char = '"',
    val hasHeader: Boolean = true,
    val encoding: String = "UTF-8",
)
