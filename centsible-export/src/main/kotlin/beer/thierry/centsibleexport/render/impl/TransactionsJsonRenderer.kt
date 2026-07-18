package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.RenderLimits
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TransactionsJsonRenderer(
    private val data: IExportDataRepository,
    private val limits: RenderLimits,
    objectMapper: ObjectMapper,
) : JsonExportRenderer(objectMapper) {
    override fun supports(): ExportType = ExportType.TRANSACTIONS

    override fun buildJson(request: ExportRequest): Any {
        val (userId, accountIds, fromDate, toDate, categoryIds) = request.transactionFilters()

        val transactions = data.fetchTransactionsCapped(
            userId, accountIds, fromDate, toDate, categoryIds, limits.maxTransactionRows,
        )

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
