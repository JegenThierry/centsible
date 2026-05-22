package beer.thierry.centsible.imports.core

import org.springframework.stereotype.Component

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
     */
    fun detect(bytes: ByteArray, filename: String, mimeType: String?): FileFormatParser? {
        val extension = filename.substringAfterLast('.', "").lowercase()
        if (extension.isNotEmpty()) {
            parsersById.values.firstOrNull { extension in it.supportedExtensions }?.let { return it }
        }
        if (!mimeType.isNullOrBlank()) {
            parsersById.values.firstOrNull { mimeType in it.supportedMimeTypes }?.let { return it }
        }
        return parsersById.values.firstOrNull { it.sniff(bytes, filename) }
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
