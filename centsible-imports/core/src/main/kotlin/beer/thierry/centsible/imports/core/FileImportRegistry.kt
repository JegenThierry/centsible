package beer.thierry.centsible.imports.core

import org.springframework.stereotype.Component

/**
 * Extensions and MIME types that say "text" and nothing more. Several banks deliver OFX or QIF
 * statements under them, so a parser claiming one of these has not actually identified the file
 * and must back its claim with a content sniff.
 */
private val AMBIGUOUS_EXTENSIONS = setOf("txt")
private val AMBIGUOUS_MIME_TYPES = setOf("text/plain")

/**
 * Aggregates every [FileFormatParser] and [CsvBankProfile] discovered via Spring component
 * scanning. Adding a new format or bank profile = drop the implementation in any module that
 * centsible-rest depends on; no registration code changes here.
 */
@Component
class FileImportRegistry(
    parsers: List<FileFormatParser>,
    profiles: List<CsvBankProfile>,
) {
    private val parsersById: Map<String, FileFormatParser> = parsers.associateBy { it.id }
    private val profilesById: Map<String, CsvBankProfile> = profiles.associateBy { it.id }

    fun listParsers(): Collection<FileFormatParser> = parsersById.values

    fun parser(id: String): FileFormatParser? = parsersById[id]

    /**
     * Identify the parser for an uploaded file. Order: extension match, then MIME match, then
     * content sniff. Returns null if no parser claims the file.
     *
     * A match on an ambiguous extension or MIME type ([AMBIGUOUS_EXTENSIONS],
     * [AMBIGUOUS_MIME_TYPES]) does not win outright: the content sniff runs first and its verdict
     * takes precedence, so a bank's OFX statement named `.txt` reaches the OFX parser instead of
     * being column-mapped as CSV. When nothing sniffs positively the ambiguous match still stands,
     * which keeps a plain `.txt` CSV — no delimiter on the header line, nothing to sniff — working.
     */
    fun detect(bytes: ByteArray, filename: String, mimeType: String?): FileFormatParser? {
        val extension = filename.substringAfterLast('.', "").lowercase()
        val mime = mimeType?.takeIf { it.isNotBlank() }.orEmpty()

        val byExtension = if (extension.isEmpty()) null
        else parsersById.values.firstOrNull { extension in it.supportedExtensions }
        if (byExtension != null && extension !in AMBIGUOUS_EXTENSIONS) return byExtension

        val byMimeType = if (mime.isEmpty()) null
        else parsersById.values.firstOrNull { mime in it.supportedMimeTypes }
        if (byMimeType != null && mime !in AMBIGUOUS_MIME_TYPES) return byMimeType

        return parsersById.values.firstOrNull { it.sniff(bytes, filename) } ?: byExtension ?: byMimeType
    }

    fun listProfiles(): Collection<CsvBankProfile> = profilesById.values

    fun profile(id: String): CsvBankProfile? = profilesById[id]

    /**
     * Score every profile against the given CSV header/sample and return the winner. Used by
     * the wizard's "we think this is a Revolut export" suggestion.
     */
    fun bestProfileMatch(header: List<String>, sample: List<List<String>>): CsvBankProfile? {
        val scored = profilesById.values
            .asSequence()
            .map { it to it.matches(header, sample) }
            .filter { it.second > MatchScore.NO_MATCH }
            .toList()
        if (scored.isEmpty()) return null
        return scored.maxByOrNull { it.second.value }?.first
    }
}
