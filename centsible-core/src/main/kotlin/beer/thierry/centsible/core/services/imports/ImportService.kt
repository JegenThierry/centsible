package beer.thierry.centsible.core.services.imports

import beer.thierry.centsible.api.model.transaction.ImportResult
import beer.thierry.centsible.api.model.transaction.ImportTransactionsRequest
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.imports.IImportService
import beer.thierry.centsible.api.services.imports.ImportDetection
import beer.thierry.centsible.api.services.imports.ImportParseWarning
import beer.thierry.centsible.api.services.imports.ImportPreview
import beer.thierry.centsible.api.services.imports.ParseHintsDTO
import beer.thierry.centsible.api.services.transactions.ITransactionService
import beer.thierry.centsible.imports.core.FileImportRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ImportService(
    private val registry: FileImportRegistry,
    private val transactionService: ITransactionService,
) : IImportService {

    private val log = LoggerFactory.getLogger(ImportService::class.java)

    override fun detect(bytes: ByteArray, filename: String, mimeType: String?): ImportDetection? {
        val parser = registry.detect(bytes, filename, mimeType) ?: return null
        return ImportDetection(
            parserId = parser.id,
            displayName = parser.displayName,
            requiresMapping = parser.requiresMapping,
        )
    }

    override fun preview(bytes: ByteArray, parserId: String, hints: ParseHintsDTO, maxRows: Int): ImportPreview {
        val parser = registry.parser(parserId)
            ?: throw IllegalArgumentException("Unknown parser '$parserId'")
        val parsed = parser.parse(bytes, ImportMappersImpl.toCore(hints))
        return ImportPreview(
            totalRows = parsed.rows.size,
            sample = parsed.rows.take(maxRows),
            warnings = parsed.warnings.map { ImportParseWarning(it.code, it.message, it.sourceRow) },
        )
    }

    override fun commit(
        accountId: UUID,
        bytes: ByteArray,
        parserId: String,
        hints: ParseHintsDTO,
        authenticatedUser: UserDTO,
    ): ImportResult {
        val parser = registry.parser(parserId)
            ?: throw IllegalArgumentException("Unknown parser '$parserId'")
        val parsed = parser.parse(bytes, ImportMappersImpl.toCore(hints))
        if (parsed.rows.size > MAX_ROWS_PER_IMPORT) {
            throw IllegalArgumentException(
                "File contains ${parsed.rows.size} rows; the per-import limit is $MAX_ROWS_PER_IMPORT. " +
                    "Split the file or import in batches."
            )
        }
        if (parsed.rows.isEmpty()) {
            log.info(
                "Import commit produced no rows accountId={} userId={} parser={}",
                accountId, authenticatedUser.id, parserId,
            )
            return ImportResult(imported = 0, skippedDuplicates = 0)
        }
        val request = ImportTransactionsRequest(rows = parsed.rows.toMutableList())
        val result = transactionService.importBatch(accountId, request, authenticatedUser)
        log.info(
            "Import committed accountId={} userId={} parser={} totalRows={} imported={} skippedDuplicates={} warnings={}",
            accountId, authenticatedUser.id, parserId,
            parsed.rows.size, result.imported, result.skippedDuplicates, parsed.warnings.size,
        )
        return result
    }

    private companion object {
        // Matches ImportTransactionsRequest.@Size(max = 1000); kept here for the clearer error message.
        const val MAX_ROWS_PER_IMPORT = 1000
    }
}
