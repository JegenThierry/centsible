package beer.thierry.centsible.imports.csv.profiles

import beer.thierry.centsible.imports.core.CsvBankProfile
import beer.thierry.centsible.imports.core.CsvColumnMapping
import beer.thierry.centsible.imports.core.MatchScore
import beer.thierry.centsible.imports.core.anyEquals
import beer.thierry.centsible.imports.core.normalizedHeader
import org.springframework.stereotype.Component
import java.util.Locale

/**
 * Revolut personal CSV export. Header:
 *   Type,Product,Started Date,Completed Date,Description,Amount,Fee,Currency,State,Balance
 *
 * Single signed amount column; debits are NEGATIVE. Independent of locale because Revolut
 * exports always use English headers and ISO dates.
 */
@Component
class RevolutProfile : CsvBankProfile {
    override val id = "revolut"
    override val displayName = "Revolut"
    override val locale: Locale? = null
    override val version = 1

    override fun matches(header: List<String>, sample: List<List<String>>): MatchScore {
        val normalized = header.normalizedHeader()
        val hasStarted = normalized.anyEquals("started date")
        val hasCompleted = normalized.anyEquals("completed date")
        val hasState = normalized.anyEquals("state")
        val hasBalance = normalized.anyEquals("balance")
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
