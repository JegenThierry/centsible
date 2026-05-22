package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class LendingsAllCsvRenderer(
    private val data: IExportDataRepository,
) : CsvExportRenderer() {
    override fun supports(): ExportType = ExportType.LENDINGS_ALL

    override fun buildCsv(request: ExportRequest): CsvBuilder {
        require(request.hasLendingsAll()) { "ExportRequest missing lendings_all body" }
        val userId = UUID.fromString(request.meta.userId)
        val includeSettled = request.lendingsAll.includeSettled

        val loans = data.fetchAllLoans(userId, includeSettled)

        val builder = CsvBuilder()
        builder.row(
            "Contact", "Loan Date", "Due Date", "Description",
            "Lent Amount", "Owed Amount", "Total Repaid", "Outstanding", "Settled",
        )
        for (l in loans) {
            builder.row(
                l.contactName,
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

    override fun filenameStem(request: ExportRequest): String = "lendings-all"
}
