package beer.thierry.centsible.api.services.imports

import beer.thierry.centsible.api.model.transaction.ImportResult
import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

/**
 * Format-agnostic facade over the centsible-imports file-parser SPI. CSV-specific bits (column
 * probe, bank-profile suggestion) live on the [ImportsResource] in centsible-rest because they
 * leak format details that the rest of the codebase shouldn't depend on.
 */
interface IImportService {

    /** Identifies which file-format parser owns the given upload, or null if none. */
    fun detect(bytes: ByteArray, filename: String, mimeType: String?): ImportDetection?

    /**
     * Parse the file without persisting. Returns the row count, the first [maxRows] for the
     * wizard's confirmation step, and any non-fatal warnings the parser surfaced.
     */
    fun preview(
        bytes: ByteArray,
        parserId: String,
        hints: ParseHintsDTO,
        maxRows: Int = 50,
    ): ImportPreview

    /** Parse and persist via the existing transactions import pipeline. */
    fun commit(
        accountId: UUID,
        bytes: ByteArray,
        parserId: String,
        hints: ParseHintsDTO,
        authenticatedUser: UserDTO,
    ): ImportResult
}

data class ImportDetection(
    val parserId: String,
    val displayName: String,
    val requiresMapping: Boolean,
)

data class ImportPreview(
    val totalRows: Int,
    val sample: List<ImportTransactionRow>,
    val warnings: List<ImportParseWarning>,
)

data class ImportParseWarning(
    val code: String,
    val message: String,
    val sourceRow: Int? = null,
)
