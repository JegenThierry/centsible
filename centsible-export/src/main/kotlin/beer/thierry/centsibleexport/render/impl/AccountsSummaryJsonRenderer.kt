package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class AccountsSummaryJsonRenderer(
    private val data: IExportDataRepository,
    objectMapper: ObjectMapper,
) : JsonExportRenderer(objectMapper) {
    override fun supports(): ExportType = ExportType.ACCOUNTS_SUMMARY

    override fun buildJson(request: ExportRequest): Any {
        require(request.hasAccountsSummary()) { "ExportRequest missing accounts_summary body" }
        val userId = UUID.fromString(request.meta.userId)
        val asOfDate = request.accountsSummary.asOfDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)

        val accounts = data.fetchAllAccounts(userId, asOfDate)

        return envelopeHeader(request, "accounts_summary") + mapOf(
            "asOf" to asOfDate?.toString(),
            "accounts" to accounts.map {
                mapOf(
                    "id" to it.id.toString(),
                    "name" to it.name,
                    "currency" to it.currency,
                    "initialBalance" to it.initialBalance,
                    "balance" to it.balance,
                    "createdAt" to it.createdAt.toString(),
                )
            },
        )
    }

    override fun filenameStem(request: ExportRequest): String = "accounts-summary"
}
