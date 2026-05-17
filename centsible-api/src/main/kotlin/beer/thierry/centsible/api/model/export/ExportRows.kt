package beer.thierry.centsible.api.model.export

import beer.thierry.centsible.api.model.category.CategoryType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class ExportTransactionRow(
    val id: UUID,
    val accountId: UUID,
    val accountName: String,
    val currency: String,
    val transactionDate: LocalDate,
    val amount: BigDecimal,
    val description: String?,
    val categoryName: String,
    val categoryType: CategoryType,
    val categoryColor: String,
)

data class ExportAccountRow(
    val id: UUID,
    val name: String,
    val currency: String,
    val initialBalance: BigDecimal,
    val balance: BigDecimal,
    val createdAt: OffsetDateTime,
)

data class ExportLoanRow(
    val id: UUID,
    val contactId: UUID,
    val contactName: String,
    val lentAmount: BigDecimal,
    val owedAmount: BigDecimal,
    val totalRepaid: BigDecimal,
    val outstanding: BigDecimal,
    val loanDate: LocalDate,
    val dueDate: LocalDate?,
    val description: String?,
    val createdAt: OffsetDateTime,
)

data class ExportContactSummaryRow(
    val contactId: UUID,
    val contactName: String,
    val totalLent: BigDecimal,
    val totalOwed: BigDecimal,
    val totalRepaid: BigDecimal,
    val outstanding: BigDecimal,
    val openLoanCount: Int,
)
