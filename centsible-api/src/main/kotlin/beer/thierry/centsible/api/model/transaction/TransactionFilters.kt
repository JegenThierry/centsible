package beer.thierry.centsible.api.model.transaction

import beer.thierry.centsible.api.model.category.CategoryType
import java.time.LocalDate

enum class TransactionSort {
    DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC
}

data class TransactionFilters(
    val search: String? = null,
    val categoryIds: List<Long>? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val type: CategoryType? = null,
    val sort: TransactionSort = TransactionSort.DATE_DESC,
)
