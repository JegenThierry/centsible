package beer.thierry.centsible.imports.csv.profiles

import beer.thierry.centsible.imports.core.CsvBankProfile
import beer.thierry.centsible.imports.core.CsvColumnMapping
import beer.thierry.centsible.imports.core.MatchScore
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class GenericCsvProfile : CsvBankProfile {
    override val id = "generic"
    override val displayName = "Generic CSV"
    override val locale: Locale? = null
    override val version = 1

    override fun matches(header: List<String>, sample: List<List<String>>): MatchScore {
        val normalized = header.map { it.lowercase() }
        val hasDate = normalized.any { DATE_TERMS.any(it::contains) }
        val hasAmount = normalized.any { AMOUNT_TERMS.any(it::contains) }
        val hasDescription = normalized.any { DESCRIPTION_TERMS.any(it::contains) }
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
