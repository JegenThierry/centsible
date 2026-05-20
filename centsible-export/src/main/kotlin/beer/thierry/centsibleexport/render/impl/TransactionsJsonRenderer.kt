package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class TransactionsJsonRenderer(
    private val data: IExportDataRepository,
    objectMapper: ObjectMapper,
) : JsonExportRenderer(objectMapper) {
    override fun supports(): ExportType = ExportType.TRANSACTIONS

    override fun buildJson(request: ExportRequest): Any {
        require(request.hasTransactions()) { "ExportRequest missing transactions body" }
        val body = request.transactions
        val userId = UUID.fromString(request.meta.userId)
        val accountIds = body.accountIdsList.map(UUID::fromString)
        val fromDate = body.fromDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        val toDate = body.toDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        val categoryIds = body.categoryIdsList.toList()

        val transactions = data.fetchTransactionsForExport(userId, accountIds, fromDate, toDate, categoryIds)

        return envelopeHeader(request, "transactions") + mapOf(
            "filters" to mapOf(
                "accountIds" to accountIds.map(UUID::toString),
                "from" to fromDate?.toString(),
                "to" to toDate?.toString(),
                "categoryIds" to categoryIds,
            ),
            "transactions" to transactions.map {
                mapOf(
                    "date" to it.transactionDate.toString(),
                    "accountId" to it.accountId.toString(),
                    "accountName" to it.accountName,
                    "currency" to it.currency,
                    "amount" to it.amount,
                    "description" to it.description,
                    "categoryName" to it.categoryName,
                    "categoryType" to it.categoryType.name,
                )
            },
        )
    }

    override fun filenameStem(request: ExportRequest): String = "transactions"
}
