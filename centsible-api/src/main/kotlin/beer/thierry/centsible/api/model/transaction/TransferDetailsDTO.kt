package beer.thierry.centsible.api.model.transaction

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

/** The two legs sharing a [transferGroupId] collapsed into one source-to-destination transfer view. */
data class TransferDetailsDTO(
    var transferGroupId: UUID,
    var sourceAccountId: UUID,
    var destinationAccountId: UUID,
    var amount: BigDecimal,
    var description: String?,
    var transactionDate: LocalDate,
)
