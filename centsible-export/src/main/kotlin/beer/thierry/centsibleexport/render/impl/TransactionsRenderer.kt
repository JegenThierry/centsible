package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.export.ExportTransactionRow
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.ExportRenderer
import beer.thierry.centsibleexport.render.PdfRenderer
import beer.thierry.centsibleexport.render.RenderLimits
import beer.thierry.centsibleexport.render.RenderedExport
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

@Component
class TransactionsRenderer(
    private val data: IExportDataRepository,
    private val pdfRenderer: PdfRenderer,
    private val limits: RenderLimits,
) : ExportRenderer {

    override fun supports(): ExportType = ExportType.TRANSACTIONS

    override fun render(request: ExportRequest): RenderedExport {
        val (userId, accountIds, fromDate, toDate, categoryIds) = request.transactionFilters()

        val transactions = data.fetchTransactionsCapped(
            userId, accountIds, fromDate, toDate, categoryIds, limits.maxTransactionRows,
        )
        val accounts = data.fetchAccountsByIds(userId, accountIds.ifEmpty { transactions.map { it.accountId }.distinct() })
        val locale = request.locale()

        val income = transactions.filter { it.categoryType == CategoryType.INCOME }.sumAmount()
        val expense = transactions.filter { it.categoryType == CategoryType.EXPENSE }.sumAmount()
        val net = income - expense

        val summaryCurrency = accounts.map { it.currency }.distinct().singleOrNull()
            ?: request.meta.currency.ifBlank { "EUR" }

        val byAccount = transactions.groupBy { it.accountId }
        val perAccount = accounts.map { acc ->
            val rows = byAccount[acc.id].orEmpty()
            mapOf(
                "name" to acc.name,
                "currency" to acc.currency,
                "income" to rows.filter { it.categoryType == CategoryType.INCOME }.sumAmount(),
                "expense" to rows.filter { it.categoryType == CategoryType.EXPENSE }.sumAmount(),
                "count" to rows.size,
            )
        }

        val subtitle = when {
            fromDate != null && toDate != null -> "Period ${formatDate(fromDate, locale)} – ${formatDate(toDate, locale)}"
            fromDate != null -> "From ${formatDate(fromDate, locale)}"
            toDate != null -> "Up to ${formatDate(toDate, locale)}"
            else -> "All transactions"
        }

        val context = mapOf<String, Any?>(
            "meta" to baseMeta(request),
            "title" to "Transactions",
            "subtitle" to subtitle,
            "transactions" to transactions.map { it.asTemplateMap(locale) },
            "perAccount" to perAccount,
            "totals" to mapOf(
                "income" to income,
                "expense" to expense,
                "net" to net,
                "count" to transactions.size,
                "currency" to summaryCurrency,
            ),
        )

        return pdfRenderer.renderExport(
            template = "transactions.peb",
            filenameStem = "transactions-${LocalDate.now()}",
            context = context,
        )
    }

    private fun List<ExportTransactionRow>.sumAmount(): BigDecimal =
        sumOf { it.amount }

    private fun ExportTransactionRow.asTemplateMap(locale: Locale): Map<String, Any?> = mapOf(
        "date" to formatDate(transactionDate, locale),
        "account" to accountName,
        "currency" to currency,
        "amount" to amount,
        "description" to (description ?: ""),
        "category" to categoryName,
        "categoryType" to categoryType.name,
        "categoryColor" to categoryColor,
        "isIncome" to (categoryType == CategoryType.INCOME),
    )
}
