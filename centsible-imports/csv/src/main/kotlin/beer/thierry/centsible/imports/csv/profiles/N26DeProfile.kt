package beer.thierry.centsible.imports.csv.profiles

import beer.thierry.centsible.imports.core.CsvBankProfile
import beer.thierry.centsible.imports.core.CsvColumnMapping
import beer.thierry.centsible.imports.core.MatchScore
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class N26DeProfile : CsvBankProfile {
    override val id = "n26-de"
    override val displayName = "N26 (Germany)"
    override val locale: Locale = Locale.GERMANY
    override val version = 1

    override fun matches(header: List<String>, sample: List<List<String>>): MatchScore {
        val normalized = header.map { it.lowercase() }
        val hasBookingDate = normalized.any { it.contains("booking date") || it == "buchungstag" }
        val hasAmountEur = normalized.any { it.contains("amount (eur)") || it.contains("betrag (eur)") }
        val hasPartner = normalized.any { it.contains("partner name") || it.contains("empfänger") }
        return when {
            hasBookingDate && hasAmountEur && hasPartner -> MatchScore.EXACT
            hasBookingDate && hasAmountEur -> MatchScore.STRONG
            else -> MatchScore.NO_MATCH
        }
    }

    override fun toMapping(): CsvColumnMapping = CsvColumnMapping(
        dateColumn = 0,
        descriptionColumn = 2,
        amountColumn = 7,
        counterpartyColumn = 2,
        dateFormat = "yyyy-MM-dd",
        decimalSeparator = '.',
        debitsArePositive = false,
    )
}
