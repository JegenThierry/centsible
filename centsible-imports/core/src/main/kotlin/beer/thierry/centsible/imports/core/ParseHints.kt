package beer.thierry.centsible.imports.core

import java.util.Locale

data class ParseHints(
    val locale: Locale? = null,
    val targetCurrency: String? = null,
    val defaultCategoryId: Long? = null,
    val csvMapping: CsvColumnMapping? = null,
    val csvDialect: CsvDialect? = null,
)

data class CsvDialect(
    val delimiter: Char = ',',
    val quote: Char = '"',
    val hasHeader: Boolean = true,
    val encoding: String = "UTF-8",
)
