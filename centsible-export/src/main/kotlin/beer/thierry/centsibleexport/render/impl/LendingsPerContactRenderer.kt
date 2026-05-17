package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportLoanRow
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.ExportRenderer
import beer.thierry.centsibleexport.render.PdfRenderer
import beer.thierry.centsibleexport.render.RenderedExport
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

@Component
class LendingsPerContactRenderer(
    private val data: IExportDataRepository,
    private val pdfRenderer: PdfRenderer,
) : ExportRenderer {

    override fun supports(): ExportType = ExportType.LENDINGS_PER_CONTACT

    override fun render(request: ExportRequest): RenderedExport {
        require(request.hasLendingsPerContact()) { "ExportRequest missing lendings_per_contact body" }
        val userId = UUID.fromString(request.meta.userId)
        val contactId = UUID.fromString(request.lendingsPerContact.contactId)

        val summary = data.fetchContactSummary(userId, contactId)
            ?: error("Contact $contactId not found for user $userId")
        val loans = data.fetchLoansForContact(userId, contactId)
        val locale = request.locale()

        val context = mapOf<String, Any?>(
            "meta" to baseMeta(request),
            "title" to "Lendings — ${summary.contactName}",
            "subtitle" to "All loans and outstanding balance",
            "summary" to mapOf(
                "totalLent" to summary.totalLent,
                "totalOwed" to summary.totalOwed,
                "totalRepaid" to summary.totalRepaid,
                "outstanding" to summary.outstanding,
                "openLoanCount" to summary.openLoanCount,
            ),
            "loans" to loans.map { it.asTemplateMap(locale) },
        )

        return pdfRenderer.renderExport(
            template = "lendings-per-contact.peb",
            filenameStem = "lendings-${slug(summary.contactName)}",
            context = context,
        )
    }
}

internal fun ExportLoanRow.asTemplateMap(locale: Locale): Map<String, Any?> = mapOf(
    "loanDate" to formatDate(loanDate, locale),
    "dueDate" to formatDate(dueDate, locale),
    "description" to (description ?: ""),
    "lentAmount" to lentAmount,
    "owedAmount" to owedAmount,
    "totalRepaid" to totalRepaid,
    "outstanding" to outstanding,
    "isSettled" to (outstanding <= BigDecimal.ZERO),
    "contactName" to contactName,
)
