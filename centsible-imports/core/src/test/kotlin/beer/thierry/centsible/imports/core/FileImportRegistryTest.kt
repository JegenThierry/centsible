package beer.thierry.centsible.imports.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.Locale

private class FakeParser(
    override val id: String,
    override val supportedExtensions: Set<String> = emptySet(),
    override val supportedMimeTypes: Set<String> = emptySet(),
    private val sniffMatches: Boolean = false,
) : FileFormatParser {
    override val displayName: String get() = id.uppercase()
    override val requiresMapping: Boolean get() = false

    override fun sniff(bytes: ByteArray, filename: String): Boolean = sniffMatches

    override fun parse(bytes: ByteArray, hints: ParseHints): ParsedFile =
        ParsedFile(rows = emptyList())
}

private class FakeProfile(
    override val id: String,
    override val displayName: String = id,
    override val locale: Locale? = null,
    override val version: Int = 1,
    private val score: MatchScore = MatchScore.NO_MATCH,
) : CsvBankProfile {
    override fun matches(header: List<String>, sample: List<List<String>>): MatchScore = score
    override fun toMapping(): CsvColumnMapping =
        CsvColumnMapping(dateColumn = 0, descriptionColumn = 1, amountColumn = 2)
}

class FileImportRegistryTest {

    @Test
    fun `listParsers and parser lookup by id`() {
        val csv = FakeParser(id = "csv")
        val ofx = FakeParser(id = "ofx")

        val registry = FileImportRegistry(parsers = listOf(csv, ofx), profiles = emptyList())

        assertEquals(2, registry.listParsers().size)
        assertSame(csv, registry.parser("csv"))
        assertSame(ofx, registry.parser("ofx"))
        assertNull(registry.parser("nope"))
    }

    @Test
    fun `detect prefers extension match over MIME and sniff`() {
        val csv = FakeParser(id = "csv", supportedExtensions = setOf("csv"))
        val other = FakeParser(
            id = "other",
            supportedMimeTypes = setOf("text/csv"),
            sniffMatches = true,
        )

        val registry = FileImportRegistry(parsers = listOf(other, csv), profiles = emptyList())

        val detected = registry.detect(
            bytes = "anything".toByteArray(),
            filename = "statement.csv",
            mimeType = "text/csv",
        )
        assertSame(csv, detected)
    }

    @Test
    fun `detect falls back to MIME match when extension is unknown`() {
        val ofx = FakeParser(
            id = "ofx",
            supportedExtensions = setOf("ofx"),
            supportedMimeTypes = setOf("application/x-ofx"),
        )

        val registry = FileImportRegistry(parsers = listOf(ofx), profiles = emptyList())

        val detected = registry.detect(
            bytes = ByteArray(0),
            filename = "download-no-extension",
            mimeType = "application/x-ofx",
        )
        assertSame(ofx, detected)
    }

    @Test
    fun `detect falls back to sniff when extension and MIME do not match`() {
        val sniffer = FakeParser(id = "sniffer", sniffMatches = true)

        val registry = FileImportRegistry(parsers = listOf(sniffer), profiles = emptyList())

        val detected = registry.detect(
            bytes = "<OFX>".toByteArray(),
            filename = "mystery.bin",
            mimeType = "application/octet-stream",
        )
        assertSame(sniffer, detected)
    }

    @Test
    fun `detect lets a sniff override an ambiguous txt extension and text-plain MIME`() {
        val csv = FakeParser(
            id = "csv",
            supportedExtensions = setOf("csv", "tsv", "txt"),
            supportedMimeTypes = setOf("text/csv", "text/plain"),
        )
        val ofx = FakeParser(
            id = "ofx",
            supportedExtensions = setOf("ofx"),
            sniffMatches = true,
        )

        val registry = FileImportRegistry(parsers = listOf(csv, ofx), profiles = emptyList())

        val detected = registry.detect(
            bytes = "OFXHEADER:100\nDATA:OFXSGML\n\n<OFX><BANKMSGSRSV1>".toByteArray(),
            filename = "statement.txt",
            mimeType = "text/plain",
        )
        assertSame(ofx, detected)
    }

    @Test
    fun `detect keeps an ambiguous txt extension match when nothing sniffs`() {
        val csv = FakeParser(
            id = "csv",
            supportedExtensions = setOf("csv", "tsv", "txt"),
            supportedMimeTypes = setOf("text/csv", "text/plain"),
        )
        val ofx = FakeParser(id = "ofx", supportedExtensions = setOf("ofx"))

        val registry = FileImportRegistry(parsers = listOf(csv, ofx), profiles = emptyList())

        val detected = registry.detect(
            bytes = "Date,Description,Amount\n2026-01-15,Coffee,-3.50".toByteArray(),
            filename = "export.txt",
            mimeType = "text/plain",
        )
        assertSame(csv, detected)
    }

    @Test
    fun `detect returns null when nothing claims the file`() {
        val csv = FakeParser(id = "csv", supportedExtensions = setOf("csv"))

        val registry = FileImportRegistry(parsers = listOf(csv), profiles = emptyList())

        val detected = registry.detect(
            bytes = "{}".toByteArray(),
            filename = "data.json",
            mimeType = "application/json",
        )
        assertNull(detected)
    }

    @Test
    fun `detect treats null and blank MIME the same as missing`() {
        val csv = FakeParser(id = "csv", supportedExtensions = setOf("csv"))

        val registry = FileImportRegistry(parsers = listOf(csv), profiles = emptyList())

        assertNull(
            registry.detect(
                bytes = ByteArray(0),
                filename = "file.unknown",
                mimeType = null,
            )
        )
        assertNull(
            registry.detect(
                bytes = ByteArray(0),
                filename = "file.unknown",
                mimeType = "   ",
            )
        )
    }

    @Test
    fun `bestProfileMatch returns the highest scoring profile`() {
        val weak = FakeProfile(id = "weak", score = MatchScore.WEAK)
        val strong = FakeProfile(id = "strong", score = MatchScore.STRONG)
        val noMatch = FakeProfile(id = "noMatch", score = MatchScore.NO_MATCH)

        val registry = FileImportRegistry(
            parsers = emptyList(),
            profiles = listOf(weak, strong, noMatch),
        )

        val best = registry.bestProfileMatch(header = listOf("Date", "Amount"), sample = emptyList())
        assertNotNull(best)
        assertEquals("strong", best?.id)
    }

    @Test
    fun `bestProfileMatch returns null when every profile reports NO_MATCH`() {
        val registry = FileImportRegistry(
            parsers = emptyList(),
            profiles = listOf(
                FakeProfile(id = "a"),
                FakeProfile(id = "b"),
            ),
        )

        assertNull(registry.bestProfileMatch(header = emptyList(), sample = emptyList()))
    }

    @Test
    fun `listProfiles and profile lookup by id`() {
        val p = FakeProfile(id = "revolut")
        val registry = FileImportRegistry(parsers = emptyList(), profiles = listOf(p))

        assertEquals(1, registry.listProfiles().size)
        assertSame(p, registry.profile("revolut"))
        assertNull(registry.profile("unknown"))
    }
}
