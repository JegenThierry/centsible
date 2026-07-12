package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class LendingsPerContactJsonRenderer(
    private val data: IExportDataRepository,
    objectMapper: ObjectMapper,
) : JsonExportRenderer(objectMapper) {
    override fun supports(): ExportType = ExportType.LENDINGS_PER_CONTACT

    override fun buildJson(request: ExportRequest): Any {
        require(request.hasLendingsPerContact()) { "ExportRequest missing lendings_per_contact body" }
        val userId = UUID.fromString(request.meta.userId)
        val contactId = UUID.fromString(request.lendingsPerContact.contactId)

        val summary = data.fetchContactSummary(userId, contactId)
            ?: error("Contact $contactId not found for user $userId")
        val loans = data.fetchLoansForContact(userId, contactId)

        return envelopeHeader(request, "lendings_per_contact") + mapOf(
            "contact" to mapOf(
                "id" to contactId.toString(),
                "name" to summary.contactName,
                "totalLent" to summary.totalLent,
                "totalOwed" to summary.totalOwed,
                "totalRepaid" to summary.totalRepaid,
                "outstanding" to summary.outstanding,
                "openLoanCount" to summary.openLoanCount,
            ),
            "loans" to loans.map {
                mapOf(
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

    override fun filenameStem(request: ExportRequest): String =
        lendingsPerContactStem(data, request)
}
