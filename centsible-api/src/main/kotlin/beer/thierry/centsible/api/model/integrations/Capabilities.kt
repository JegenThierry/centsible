package beer.thierry.centsible.api.model.integrations

import java.math.BigDecimal
import java.time.OffsetDateTime

data class ExternalAccountDTO(
    val externalId: String,
    val name: String,
    val type: String? = null,
    val currency: String? = null,
    val balance: BigDecimal? = null,
)

data class ImportedTransactionDTO(
    val externalId: String,
    val externalAccountId: String,
    val amount: BigDecimal,
    val currency: String? = null,
    val description: String,
    val occurredAt: OffsetDateTime,
    val counterparty: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)

data class TransactionImportPage(
    val transactions: List<ImportedTransactionDTO>,
    val nextCursor: String? = null,
)

data class QuoteDTO(
    val symbol: String,
    val price: BigDecimal,
    val currency: String,
    val asOf: OffsetDateTime,
)
