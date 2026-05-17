package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.ExportRenderer
import beer.thierry.centsibleexport.render.PdfRenderer
import beer.thierry.centsibleexport.render.RenderedExport
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Component
class AccountsSummaryRenderer(
    private val data: IExportDataRepository,
    private val pdfRenderer: PdfRenderer,
) : ExportRenderer {

    override fun supports(): ExportType = ExportType.ACCOUNTS_SUMMARY

    override fun render(request: ExportRequest): RenderedExport {
        require(request.hasAccountsSummary()) { "ExportRequest missing accounts_summary body" }
        val userId = UUID.fromString(request.meta.userId)
        val asOfDate = request.accountsSummary.asOfDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)

        val accounts = data.fetchAllAccounts(userId, asOfDate)
        val locale = request.locale()

        val byCurrency = accounts.groupBy { it.currency }
            .map { (currency, accs) ->
                mapOf(
                    "currency" to currency,
                    "totalBalance" to accs.fold(BigDecimal.ZERO) { acc, a -> acc + a.balance },
                    "accounts" to accs.map { a ->
                        mapOf(
                            "name" to a.name,
                            "currency" to a.currency,
                            "initialBalance" to a.initialBalance,
                            "balance" to a.balance,
                            "delta" to (a.balance - a.initialBalance),
                            "createdAt" to formatDate(a.createdAt.toLocalDate(), locale),
                        )
                    },
                )
            }
            .sortedBy { it["currency"] as String }

        val context = mapOf<String, Any?>(
            "meta" to baseMeta(request),
            "title" to "Accounts Summary",
            "subtitle" to (asOfDate?.let { "Balances as of ${formatDate(it, locale)}" } ?: "Current balances"),
            "totalAccounts" to accounts.size,
            "byCurrency" to byCurrency,
        )

        return pdfRenderer.renderExport("accounts-summary.peb", "accounts-summary", context)
    }
}
