package beer.thierry.budgetplannerexport.render.impl

import beer.thierry.budgetplanner.api.model.category.CategoryType
import beer.thierry.budgetplanner.api.model.export.ExportTransactionRow
import beer.thierry.budgetplanner.api.model.export.ExportType
import beer.thierry.budgetplanner.api.repository.IExportDataRepository
import beer.thierry.budgetplanner.export.proto.ExportRequest
import beer.thierry.budgetplannerexport.render.ExportRenderer
import beer.thierry.budgetplannerexport.render.PdfRenderer
import beer.thierry.budgetplannerexport.render.RenderedExport
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class TransactionsRenderer(
    private val data: IExportDataRepository,
    private val pdfRenderer: PdfRenderer,
) : ExportRenderer {

    override fun supports(): ExportType = ExportType.TRANSACTIONS

    override fun render(request: ExportRequest): RenderedExport {
        require(request.hasTransactions()) { "ExportRequest missing transactions body" }
        val body = request.transactions
        val userId = UUID.fromString(request.meta.userId)
        val accountIds = body.accountIdsList.map(UUID::fromString)
        val fromDate = body.fromDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        val toDate = body.toDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        val categoryIds = body.categoryIdsList.toList()

        val transactions = data.fetchTransactionsForExport(userId, accountIds, fromDate, toDate, categoryIds)
        val accounts = data.fetchAccountsByIds(userId, accountIds.ifEmpty { transactions.map { it.accountId }.distinct() })
        val locale = request.locale()

        val income = transactions.filter { it.categoryType == CategoryType.INCOME }.sumAmount()
        val expense = transactions.filter { it.categoryType == CategoryType.EXPENSE }.sumAmount()
        val net = income - expense

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
            ),
        )

        val pdf = pdfRenderer.renderHtmlToPdf("transactions.peb", context)
        val filename = "transactions-${LocalDate.now()}-${OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()}.pdf"
        return RenderedExport(pdf, filename)
    }

    private fun List<ExportTransactionRow>.sumAmount(): BigDecimal =
        fold(BigDecimal.ZERO) { acc, row -> acc + row.amount }

    private fun ExportTransactionRow.asTemplateMap(locale: java.util.Locale): Map<String, Any?> = mapOf(
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
