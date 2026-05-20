package beer.thierry.centsible.imports.core

/**
 * Parses an uploaded file (CSV, OFX, ...) into a canonical [ParsedFile]. One implementation per
 * format lives in its own Gradle module (centsible-imports/csv, centsible-imports/ofx, ...) and
 * is auto-discovered by [FileImportRegistry] through Spring component scanning. Adding a new
 * format = new sub-module that registers a single bean here; no central wiring code changes.
 *
 * Implementations must be stateless and thread-safe; the registry shares one instance across
 * all imports.
 */
interface FileFormatParser {
    /** Stable id used by the UI and persisted in saved mappings. e.g. "csv", "ofx". */
    val id: String

    /** Human-readable name for the format picker. e.g. "CSV", "OFX (Open Financial Exchange)". */
    val displayName: String

    /** MIME types this parser accepts. Used during upload to short-list candidates. */
    val supportedMimeTypes: Set<String>

    /** File extensions (lowercase, without the leading dot) this parser handles. */
    val supportedExtensions: Set<String>

    /**
     * Best-effort content sniff used by [FileImportRegistry.detect] when MIME and extension are
     * inconclusive. Should only peek at the first few KB and never throw.
     */
    fun sniff(bytes: ByteArray, filename: String): Boolean

    /**
     * Parse the file into [ParsedFile]. Hints carry per-request context (locale, target-account
     * currency, CSV mapping, ...). Implementations must NOT mutate the input bytes.
     */
    fun parse(bytes: ByteArray, hints: ParseHints): ParsedFile
}
