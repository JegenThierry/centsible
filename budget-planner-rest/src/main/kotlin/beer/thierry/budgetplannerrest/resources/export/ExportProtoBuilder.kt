package beer.thierry.budgetplannerrest.resources.export

import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.export.proto.AccountsSummaryRequest
import beer.thierry.budgetplanner.export.proto.ExportMeta
import beer.thierry.budgetplanner.export.proto.ExportRequest
import beer.thierry.budgetplanner.export.proto.LendingsAllRequest
import beer.thierry.budgetplanner.export.proto.LendingsPerContactRequest
import beer.thierry.budgetplanner.export.proto.TransactionsRequest
import com.google.protobuf.Timestamp
import org.springframework.stereotype.Component
import java.time.Instant

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
                .addAllAccountIds(params.accountIds.orEmpty().map { it.toString() })
                .setFromDate(params.fromDate?.toString().orEmpty())
                .setToDate(params.toDate?.toString().orEmpty())
                .addAllCategoryIds(params.categoryIds.orEmpty())
                .build()

            is LendingsPerContactExportParams -> builder.lendingsPerContact = LendingsPerContactRequest.newBuilder()
                .setContactId(params.contactId!!.toString())
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
