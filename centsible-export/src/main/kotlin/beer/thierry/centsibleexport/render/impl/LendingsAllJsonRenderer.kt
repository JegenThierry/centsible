package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class LendingsAllJsonRenderer(
    private val data: IExportDataRepository,
    objectMapper: ObjectMapper,
) : JsonExportRenderer(objectMapper) {
    override fun supports(): ExportType = ExportType.LENDINGS_ALL

    override fun buildJson(request: ExportRequest): Any {
        require(request.hasLendingsAll()) { "ExportRequest missing lendings_all body" }
        val userId = UUID.fromString(request.meta.userId)
        val includeSettled = request.lendingsAll.includeSettled

        val loans = data.fetchAllLoans(userId, includeSettled)
        val summaries = data.fetchAllContactSummaries(userId)

        return envelopeHeader(request, "lendings_all") + mapOf(
            "includeSettled" to includeSettled,
            "summaries" to summaries.map {
                mapOf(
                    "contactId" to it.contactId.toString(),
                    "contactName" to it.contactName,
                    "totalLent" to it.totalLent,
                    "totalRepaid" to it.totalRepaid,
                    "outstanding" to it.outstanding,
                    "openLoanCount" to it.openLoanCount,
                )
            },
            "loans" to loans.map {
                mapOf(
                    "contactId" to it.contactId.toString(),
                    "contactName" to it.contactName,
                    "loanDate" to it.loanDate.toString(),
                    "dueDate" to it.dueDate?.toString(),
                    "description" to it.description,
                    "lentAmount" to it.lentAmount,
                    "owedAmount" to it.owedAmount,
                    "totalRepaid" to it.totalRepaid,
                    "outstanding" to it.outstanding,
                    "settled" to (it.outstanding <= BigDecimal.ZERO),
                )
            },
        )
    }

    override fun filenameStem(request: ExportRequest): String = "lendings-all"
}
