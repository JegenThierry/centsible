package beer.thierry.centsiblerest.resources.export

import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.export.proto.AccountsSummaryRequest
import beer.thierry.centsible.export.proto.ExportMeta
import beer.thierry.centsible.export.proto.ExportRequest
import beer.thierry.centsible.export.proto.LendingsAllRequest
import beer.thierry.centsible.export.proto.LendingsPerContactRequest
import beer.thierry.centsible.export.proto.TransactionsRequest
import com.google.protobuf.Timestamp
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class ExportProtoBuilder {

    fun build(user: UserDTO, params: ExportRequestParams, locale: String, currency: String): ByteArray {
        val meta = ExportMeta.newBuilder()
            .setUserId(user.id.toString())
            .setUserDisplayName(user.name.ifBlank { "${user.firstName} ${user.lastName}".trim() })
            .setUserEmail(user.email)
            .setLocale(locale)
            .setCurrency(currency)
            .setRequestedAt(Instant.now().toTimestamp())
            .build()

        val builder = ExportRequest.newBuilder().setMeta(meta)
        when (params) {
            is TransactionsExportParams -> builder.transactions = TransactionsRequest.newBuilder()
                .addAllAccountIds(params.accountIds.orEmpty().map(UUID::toString))
                .setFromDate(params.fromDate?.toString().orEmpty())
                .setToDate(params.toDate?.toString().orEmpty())
                .addAllCategoryIds(params.categoryIds.orEmpty())
                .build()

            is LendingsPerContactExportParams -> builder.lendingsPerContact = LendingsPerContactRequest.newBuilder()
                .setContactId(requireNotNull(params.contactId) { "contactId is required" }.toString())
                .build()

            is LendingsAllExportParams -> builder.lendingsAll = LendingsAllRequest.newBuilder()
                .setIncludeSettled(params.includeSettled ?: true)
                .build()

            is AccountsSummaryExportParams -> builder.accountsSummary = AccountsSummaryRequest.newBuilder()
                .setAsOfDate(params.asOfDate?.toString().orEmpty())
                .build()
        }

        return builder.build().toByteArray()
    }

    private fun Instant.toTimestamp(): Timestamp =
        Timestamp.newBuilder().setSeconds(epochSecond).setNanos(nano).build()
}
