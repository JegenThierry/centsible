package beer.thierry.centsible.imports.csv.profiles

import beer.thierry.centsible.imports.core.CsvBankProfile
import beer.thierry.centsible.imports.core.CsvColumnMapping
import beer.thierry.centsible.imports.core.MatchScore
import org.springframework.stereotype.Component
import java.util.Locale

@Component
class RevolutProfile : CsvBankProfile {
    override val id = "revolut"
    override val displayName = "Revolut"
    override val locale: Locale? = null
    override val version = 1

    override fun matches(header: List<String>, sample: List<List<String>>): MatchScore {
        val normalized = header.map { it.lowercase() }
        val hasStarted = normalized.any { it == "started date" }
        val hasCompleted = normalized.any { it == "completed date" }
        val hasState = normalized.any { it == "state" }
        val hasBalance = normalized.any { it == "balance" }
        return when {
            hasStarted && hasCompleted && hasState && hasBalance -> MatchScore.EXACT
            hasCompleted && hasBalance -> MatchScore.STRONG
            else -> MatchScore.NO_MATCH
        }
    }

    override fun toMapping(): CsvColumnMapping = CsvColumnMapping(
        dateColumn = 3,
        descriptionColumn = 4,
        amountColumn = 5,
        currencyColumn = 7,
        dateFormat = "yyyy-MM-dd HH:mm:ss",
        debitsArePositive = false,
    )
}
