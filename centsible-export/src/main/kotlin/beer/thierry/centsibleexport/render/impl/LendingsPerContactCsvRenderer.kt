package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class LendingsPerContactCsvRenderer(
    private val data: IExportDataRepository,
) : CsvExportRenderer() {
    override fun supports(): ExportType = ExportType.LENDINGS_PER_CONTACT

    override fun buildCsv(request: ExportRequest): CsvBuilder {
        require(request.hasLendingsPerContact()) { "ExportRequest missing lendings_per_contact body" }
        val userId = UUID.fromString(request.meta.userId)
        val contactId = UUID.fromString(request.lendingsPerContact.contactId)

        val summary = data.fetchContactSummary(userId, contactId)
            ?: error("Contact $contactId not found for user $userId")
        val loans = data.fetchLoansForContact(userId, contactId)

        val builder = CsvBuilder()
        builder.row("Contact", "Total Lent", "Total Owed", "Total Repaid", "Outstanding", "Open Loans")
        builder.row(
            summary.contactName,
            summary.totalLent.toPlainString(),
            summary.totalOwed.toPlainString(),
            summary.totalRepaid.toPlainString(),
            summary.outstanding.toPlainString(),
            summary.openLoanCount,
        )
        builder.row()
        builder.row("Loan Date", "Due Date", "Description", "Lent", "Owed", "Repaid", "Outstanding", "Settled")
        for (l in loans) {
            builder.row(
                l.loanDate,
                l.dueDate,
                l.description ?: "",
                l.lentAmount.toPlainString(),
                l.owedAmount.toPlainString(),
                l.totalRepaid.toPlainString(),
                l.outstanding.toPlainString(),
                if (l.outstanding <= BigDecimal.ZERO) "yes" else "no",
            )
        }
        return builder
    }

    override fun filenameStem(request: ExportRequest): String =
        lendingsPerContactStem(data, request)
}
