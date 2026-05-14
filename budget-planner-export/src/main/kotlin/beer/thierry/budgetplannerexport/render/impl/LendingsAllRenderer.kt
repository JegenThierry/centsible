package beer.thierry.budgetplannerexport.render.impl

import beer.thierry.budgetplanner.api.model.export.ExportType
import beer.thierry.budgetplanner.api.repository.IExportDataRepository
import beer.thierry.budgetplanner.export.proto.ExportRequest
import beer.thierry.budgetplannerexport.render.ExportRenderer
import beer.thierry.budgetplannerexport.render.PdfRenderer
import beer.thierry.budgetplannerexport.render.RenderedExport
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class LendingsAllRenderer(
    private val data: IExportDataRepository,
    private val pdfRenderer: PdfRenderer,
) : ExportRenderer {

    override fun supports(): ExportType = ExportType.LENDINGS_ALL

    override fun render(request: ExportRequest): RenderedExport {
        require(request.hasLendingsAll()) { "ExportRequest missing lendings_all body" }
        val userId = UUID.fromString(request.meta.userId)
        val includeSettled = request.lendingsAll.includeSettled

        val loans = data.fetchAllLoans(userId, includeSettled)
        val summaries = data.fetchAllContactSummaries(userId).associateBy { it.contactId }
        val locale = request.locale()

        val grouped = loans.groupBy { it.contactId }.map { (contactId, contactLoans) ->
            val summary = summaries[contactId]
            mapOf(
                "contactName" to (summary?.contactName ?: contactLoans.first().contactName),
                "outstanding" to (summary?.outstanding ?: BigDecimal.ZERO),
                "totalLent" to (summary?.totalLent ?: BigDecimal.ZERO),
                "totalRepaid" to (summary?.totalRepaid ?: BigDecimal.ZERO),
                "openLoanCount" to (summary?.openLoanCount ?: 0),
                "loans" to contactLoans.map { it.asTemplateMap(locale) },
            )
        }.sortedByDescending { (it["outstanding"] as BigDecimal) }

        val totalsOutstanding = summaries.values.sumOf { it.outstanding }
        val totalsLent = summaries.values.sumOf { it.totalLent }
        val totalsRepaid = summaries.values.sumOf { it.totalRepaid }

        val context = mapOf<String, Any?>(
            "meta" to baseMeta(request),
            "title" to "All Lendings",
            "subtitle" to if (includeSettled) "Including settled loans" else "Active loans only",
            "totals" to mapOf(
                "outstanding" to totalsOutstanding,
                "lent" to totalsLent,
                "repaid" to totalsRepaid,
                "loanCount" to loans.size,
                "contactCount" to grouped.size,
            ),
            "groups" to grouped,
        )

        val pdf = pdfRenderer.renderHtmlToPdf("lendings-all.peb", context)
        val filename = "lendings-all-${OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()}.pdf"
        return RenderedExport(pdf, filename)
    }
}
