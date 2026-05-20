package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class AccountsSummaryCsvRenderer(
    private val data: IExportDataRepository,
) : CsvExportRenderer() {
    override fun supports(): ExportType = ExportType.ACCOUNTS_SUMMARY

    override fun buildCsv(request: ExportRequest): CsvBuilder {
        require(request.hasAccountsSummary()) { "ExportRequest missing accounts_summary body" }
        val userId = UUID.fromString(request.meta.userId)
        val asOfDate = request.accountsSummary.asOfDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)

        val accounts = data.fetchAllAccounts(userId, asOfDate)

        val builder = CsvBuilder()
        builder.row("Name", "Currency", "Initial Balance", "Balance", "Delta", "Created At")
        for (a in accounts) {
            builder.row(
                a.name,
                a.currency,
                a.initialBalance.toPlainString(),
                a.balance.toPlainString(),
                (a.balance - a.initialBalance).toPlainString(),
                a.createdAt.toLocalDate(),
            )
        }
        return builder
    }

    override fun filenameStem(request: ExportRequest): String = "accounts-summary"
}
