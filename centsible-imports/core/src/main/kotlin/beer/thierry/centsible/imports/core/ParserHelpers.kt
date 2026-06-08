package beer.thierry.centsible.imports.core

import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.transaction.ImportTransactionRow
import org.slf4j.Logger
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Builds an [ImportTransactionRow] from a signed [amount]: the amount is stored unsigned (abs) and
 * its sign selects EXPENSE (negative) vs INCOME. Shared by every file-format parser so the
 * amount / [CategoryType] convention lives in one place.
 */
fun importRow(amount: BigDecimal, description: String, date: LocalDate, defaultCategoryId: Long): ImportTransactionRow =
    ImportTransactionRow(
        amount = amount.abs(),
        categoryId = defaultCategoryId,
        description = description,
        transactionDate = date,
        type = if (amount.signum() < 0) CategoryType.EXPENSE else CategoryType.INCOME,
    )

/**
 * Logs one summary line per distinct warning code (with counts) instead of per row — a malformed
 * file can emit thousands of skipped-row warnings, and per-row WARN would drown out other signal.
 */
fun logWarningSummary(format: String, warnings: List<ParseWarning>, log: Logger) {
    if (warnings.isEmpty()) return
    warnings.groupingBy { it.code }.eachCount().forEach { (code, count) ->
        log.warn("{} parse warning code={} count={}", format, code, count)
    }
}

/**
 * The default category id a parser tags unmapped rows with so they still satisfy validation; a
 * parser cannot run without it (the import service supplies it, the file does not).
 */
fun requireDefaultCategoryId(hints: ParseHints, parserName: String): Long =
    requireNotNull(hints.defaultCategoryId) {
        "$parserName requires ParseHints.defaultCategoryId so unmapped rows still satisfy validation."
    }
