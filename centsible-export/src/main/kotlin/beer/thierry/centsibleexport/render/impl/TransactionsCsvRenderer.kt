package beer.thierry.centsibleexport.render.impl

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.repository.IExportDataRepository
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsibleexport.render.RenderLimits
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class TransactionsCsvRenderer(
    private val data: IExportDataRepository,
    private val limits: RenderLimits,
) : CsvExportRenderer() {
    override fun supports(): ExportType = ExportType.TRANSACTIONS

    override fun buildCsv(request: ExportRequest): CsvBuilder {
        require(request.hasTransactions()) { "ExportRequest missing transactions body" }
        val body = request.transactions
        val userId = UUID.fromString(request.meta.userId)
        val accountIds = body.accountIdsList.map(UUID::fromString)
        val fromDate = body.fromDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        val toDate = body.toDate.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        val categoryIds = body.categoryIdsList.toList()

        val transactions = data.fetchTransactionsCapped(
            userId, accountIds, fromDate, toDate, categoryIds, limits.maxTransactionRows,
        )

        val builder = CsvBuilder()
        builder.row("Date", "Account", "Currency", "Amount", "Description", "Category", "Type")
        for (txn in transactions) {
            builder.row(
                txn.transactionDate,
                txn.accountName,
                txn.currency,
                txn.amount.toPlainString(),
                txn.description ?: "",
                txn.categoryName,
                txn.categoryType.name,
            )
        }
        return builder
    }

    override fun filenameStem(request: ExportRequest): String = "transactions"
}
