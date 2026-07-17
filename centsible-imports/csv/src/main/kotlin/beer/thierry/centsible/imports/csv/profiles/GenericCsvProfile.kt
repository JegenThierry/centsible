package beer.thierry.centsible.imports.csv.profiles

import beer.thierry.centsible.imports.core.CsvBankProfile
import beer.thierry.centsible.imports.core.CsvColumnMapping
import beer.thierry.centsible.imports.core.MatchScore
import beer.thierry.centsible.imports.core.anyContains
import beer.thierry.centsible.imports.core.normalizedHeader
import org.springframework.stereotype.Component
import java.util.Locale

/**
 * Catch-all profile that scores any CSV with a recognizable date/amount/description triad. Lets
 * the wizard prefill a reasonable mapping for banks we don't yet have a bespoke profile for.
 */
@Component
class GenericCsvProfile : CsvBankProfile {
    override val id = "generic"
    override val displayName = "Generic CSV"
    override val locale: Locale? = null
    override val version = 1

    override fun matches(header: List<String>, sample: List<List<String>>): MatchScore {
        val normalized = header.normalizedHeader()
        val hasDate = DATE_TERMS.any { normalized.anyContains(it) }
        val hasAmount = AMOUNT_TERMS.any { normalized.anyContains(it) }
        val hasDescription = DESCRIPTION_TERMS.any { normalized.anyContains(it) }
        if (hasDate && hasAmount && hasDescription) return MatchScore.WEAK
        return MatchScore.NO_MATCH
    }

    override fun toMapping(): CsvColumnMapping = CsvColumnMapping(
        dateColumn = 0,
        descriptionColumn = 1,
        amountColumn = 2,
        dateFormat = "yyyy-MM-dd",
    )

    private companion object {
        val DATE_TERMS = listOf("date", "datum", "when", "fecha")
        val AMOUNT_TERMS = listOf("amount", "value", "montant", "betrag", "total", "importe")
        val DESCRIPTION_TERMS = listOf("desc", "memo", "note", "reference", "payee", "libell", "narrative", "concepto")
    }
}
