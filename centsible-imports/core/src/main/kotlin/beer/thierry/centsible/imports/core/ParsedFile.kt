package beer.thierry.centsible.imports.core

import beer.thierry.centsible.api.model.transaction.ImportTransactionRow

data class ParsedFile(
    val rows: List<ImportTransactionRow>,
    val warnings: List<ParseWarning> = emptyList(),
    val detectedCurrency: String? = null,
    val sourceFilename: String? = null,
)

data class ParseWarning(
    val code: String,
    val message: String,
    val sourceRow: Int? = null,
)
