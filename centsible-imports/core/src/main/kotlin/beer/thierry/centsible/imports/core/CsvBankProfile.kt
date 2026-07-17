package beer.thierry.centsible.imports.core

import java.util.Locale

/**
 * Pre-configured mapping for a known bank's CSV export. Profiles are matched against the
 * header (and a small sample of rows) of an uploaded file; the highest-scoring match pre-fills
 * the column mapping so the user only has to confirm.
 *
 * Adding support for a new bank = one new class implementing this interface, dropped in
 * centsible-imports/csv (or any module on the classpath). [FileImportRegistry] auto-discovers
 * it via Spring component scanning — no central registration.
 */
interface CsvBankProfile {
    /** Stable id persisted in saved mappings. e.g. "n26-de", "revolut", "chase-checking". */
    val id: String

    val displayName: String

    /** Locale this profile expects. Used as a tie-breaker when two profiles score equally. */
    val locale: Locale?

    /**
     * Bump when the bank changes its CSV format. Saved user mappings store this version so the
     * wizard can warn "this bank changed its format — re-verify the mapping".
     */
    val version: Int

    /**
     * Score this profile against a CSV's header (and optional sample rows). Higher is better.
     * Return [MatchScore.NO_MATCH] to opt out.
     */
    fun matches(header: List<String>, sample: List<List<String>>): MatchScore

    /** The column mapping this profile recommends when it matches. */
    fun toMapping(): CsvColumnMapping
}

@JvmInline
value class MatchScore(val value: Int) : Comparable<MatchScore> {
    override fun compareTo(other: MatchScore): Int = value.compareTo(other.value)

    companion object {
        val NO_MATCH = MatchScore(0)
        val WEAK = MatchScore(25)
        val PARTIAL = MatchScore(50)
        val STRONG = MatchScore(75)
        val EXACT = MatchScore(100)
    }
}

data class CsvColumnMapping(
    val dateColumn: Int,
    val descriptionColumn: Int,
    val amountColumn: Int? = null,
    val debitColumn: Int? = null,
    val creditColumn: Int? = null,
    val currencyColumn: Int? = null,
    val counterpartyColumn: Int? = null,
    val categoryColumn: Int? = null,
    val dateFormat: String = "yyyy-MM-dd",
    val decimalSeparator: Char = '.',
    val thousandsSeparator: Char? = null,
    val debitsArePositive: Boolean = true,
)
