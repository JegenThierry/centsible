package beer.thierry.centsible.api.model.transaction

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.tag.TagDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class TransactionDTO(
    var id: UUID? = null,
    var category: CategoryDTO = CategoryDTO(),
    var type: CategoryType = CategoryType.EXPENSE,
    var amount: BigDecimal? = null,
    var description: String? = null,
    var transactionDate: LocalDate? = null,
    var createdAt: OffsetDateTime? = null,
    var updatedAt: OffsetDateTime? = null,
    var attachmentCount: Int = 0,
    var originalAmount: BigDecimal? = null,
    var originalCurrency: Currency? = null,
    var exchangeRate: BigDecimal? = null,
    var rateDate: LocalDate? = null,
    var transferGroupId: UUID? = null,
    var tags: List<TagDTO> = emptyList(),
    var splits: List<TransactionSplitDTO> = emptyList(),
)

/**
 * One slice of a split transaction: a portion of the parent transaction's amount attributed to a
 * distinct [category]. A transaction has either no splits (its own category is authoritative) or
 * two-or-more splits whose [amount]s sum to the transaction amount.
 */
data class TransactionSplitDTO(
    var id: UUID? = null,
    var category: CategoryDTO = CategoryDTO(),
    var amount: BigDecimal? = null,
    var note: String? = null,
)
