package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.transaction.ImportResult
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.imports.CsvColumnMappingDTO
import beer.thierry.centsible.api.services.imports.CsvDialectDTO
import beer.thierry.centsible.api.services.imports.IImportService
import beer.thierry.centsible.api.services.imports.ImportDetection
import beer.thierry.centsible.api.services.imports.ImportPreview
import beer.thierry.centsible.api.services.imports.ParseHintsDTO
import beer.thierry.centsible.core.services.imports.ImportMappersImpl
import beer.thierry.centsible.imports.core.FileImportRegistry
import beer.thierry.centsible.imports.core.ParseHints
import beer.thierry.centsible.imports.csv.CsvFileParser
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

private const val MAX_IMPORT_BYTES = 5L * 1024 * 1024

/**
 * REST surface for the file-import wizard. Format-agnostic endpoints (detect, preview, commit)
 * delegate to [IImportService]; CSV-specific endpoints (probe, profiles) live here because they
 * leak CSV details that the rest of the codebase doesn't need to see.
 */
@RequestMapping("/api/imports")
@RestController
class ImportsResource(
    private val importService: IImportService,
    private val registry: FileImportRegistry,
    private val csvParser: CsvFileParser,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(ImportsResource::class.java)

    @PostMapping("/detect", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun detect(@RequestParam("file") file: MultipartFile): ResponseEntity<ImportDetection> {
        val bytes = readBoundedBytes(file)
        val detection = importService.detect(bytes, file.originalFilename ?: "upload", file.contentType)
            ?: throw LocalizedException.BadRequest("error.import.unsupportedFormat", supportedExtensionList())
        return ResponseEntity.ok(detection)
    }

    /** Comma-separated, de-duplicated list of every extension a registered parser accepts (e.g. ".csv, .ofx"). */
    private fun supportedExtensionList(): String =
        registry.listParsers()
            .flatMap { it.supportedExtensions }
            .map { if (it.startsWith(".")) it else ".$it" }
            .distinct()
            .sorted()
            .joinToString(", ")

    @PostMapping("/csv/probe", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun csvProbe(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("dialect", required = false) dialectJson: String?,
    ): ResponseEntity<CsvProbeResponse> {
        val bytes = readBoundedBytes(file)
        val dialect = dialectJson?.let { objectMapper.readValue(it, CsvDialectDTO::class.java) }?.let(ImportMappersImpl::toCore)
        val probe = csvParser.probe(bytes, ParseHints(csvDialect = dialect))
        val suggested = registry.bestProfileMatch(probe.header, probe.sample)
        val mapping = suggested?.toMapping()?.let { ImportMappersImpl.toDto(it) }
        return ResponseEntity.ok(
            CsvProbeResponse(
                header = probe.header,
                sample = probe.sample,
                dialect = ImportMappersImpl.toDto(probe.detectedDialect),
                suggestedProfileId = suggested?.id,
                suggestedProfileVersion = suggested?.version,
                suggestedMapping = mapping,
            )
        )
    }

    @PostMapping("/preview", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun preview(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("parserId") parserId: String,
        @RequestParam("hints") hintsJson: String,
        @RequestParam("maxRows", required = false, defaultValue = "50") maxRows: Int,
    ): ResponseEntity<ImportPreview> {
        val bytes = readBoundedBytes(file)
        val hints = objectMapper.readValue(hintsJson, ParseHintsDTO::class.java)
        return ResponseEntity.ok(importService.preview(bytes, parserId, hints, maxRows))
    }

    @PostMapping("/{accountId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun commit(
        @PathVariable accountId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("parserId") parserId: String,
        @RequestParam("hints") hintsJson: String,
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<ImportResult> {
        val bytes = readBoundedBytes(file)
        val hints = objectMapper.readValue(hintsJson, ParseHintsDTO::class.java)
        val result = importService.commit(accountId, bytes, parserId, hints, user)
        log.info(
            "Imported file accountId={} userId={} parserId={} sizeBytes={} imported={} skipped={}",
            accountId, user.id, parserId, bytes.size, result.imported, result.skippedDuplicates,
        )
        return ResponseEntity.ok(result)
    }

    @GetMapping("/profiles")
    fun listProfiles(): ResponseEntity<List<CsvProfileSummary>> =
        ResponseEntity.ok(
            registry.listProfiles().map {
                CsvProfileSummary(
                    id = it.id,
                    displayName = it.displayName,
                    localeTag = it.locale?.toLanguageTag(),
                    version = it.version,
                )
            }
        )

    @GetMapping("/parsers")
    fun listParsers(): ResponseEntity<List<ParserSummary>> =
        ResponseEntity.ok(
            registry.listParsers().map {
                ParserSummary(
                    id = it.id,
                    displayName = it.displayName,
                    requiresMapping = it.requiresMapping,
                    extensions = it.supportedExtensions.toList().sorted(),
                )
            }
        )

    private fun readBoundedBytes(file: MultipartFile): ByteArray {
        require(!file.isEmpty) { "Uploaded file is empty." }
        require(file.size <= MAX_IMPORT_BYTES) { "Uploaded file exceeds 5 MB limit." }
        return file.bytes
    }

}

data class CsvProbeResponse(
    val header: List<String>,
    val sample: List<List<String>>,
    val dialect: CsvDialectDTO,
    val suggestedProfileId: String?,
    val suggestedProfileVersion: Int?,
    val suggestedMapping: CsvColumnMappingDTO?,
)

data class CsvProfileSummary(
    val id: String,
    val displayName: String,
    val localeTag: String?,
    val version: Int,
)

data class ParserSummary(
    val id: String,
    val displayName: String,
    val requiresMapping: Boolean,
    val extensions: List<String>,
)
