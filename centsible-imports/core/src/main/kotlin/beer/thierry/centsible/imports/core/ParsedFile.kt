package beer.thierry.centsible.imports.core

import beer.thierry.centsible.api.model.transaction.ImportTransactionRow

/**
 * Canonical output of every [FileFormatParser]. Carries the rows ready to feed into
 * TransactionService.importBatch() plus non-fatal warnings the UI can surface to the user.
 *
 * Anything that ultimately wants to land transactions in Centsible — file upload, future
 * bank-API sync, manual entry — converges on a list of [ImportTransactionRow], which is the
 * single load-bearing seam the import pipeline is built around.
 */
data class ParsedFile(
    val rows: List<ImportTransactionRow>,
    val warnings: List<ParseWarning> = emptyList(),
    /** Detected from file contents when the format carries it (e.g. OFX `CURDEF`). */
    val detectedCurrency: String? = null,
    val sourceFilename: String? = null,
)

/**
 * A non-fatal issue the parser wants to report. Fatal problems should throw instead.
 */
data class ParseWarning(
    /** Stable code so the UI can localize. e.g. "row.skipped.missing-amount". */
    val code: String,
    /** Human-readable English fallback. */
    val message: String,
    /** 1-based source row number when applicable. */
    val sourceRow: Int? = null,
)
