package beer.thierry.centsiblerest.resources.export

import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.model.export.PostProcessingType
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID

data class CreateExportRequest(
    @field:NotNull var type: ExportType? = null,
    @field:NotBlank @field:Size(max = 200) var title: String? = null,
    @field:NotNull @field:Valid var params: ExportRequestParams? = null,
    @field:Valid var postProcessing: List<PostProcessingRequest>? = null,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes(
    JsonSubTypes.Type(value = TransactionsExportParams::class, name = "TRANSACTIONS"),
    JsonSubTypes.Type(value = LendingsPerContactExportParams::class, name = "LENDINGS_PER_CONTACT"),
    JsonSubTypes.Type(value = LendingsAllExportParams::class, name = "LENDINGS_ALL"),
    JsonSubTypes.Type(value = AccountsSummaryExportParams::class, name = "ACCOUNTS_SUMMARY"),
)
sealed interface ExportRequestParams

data class TransactionsExportParams(
    var accountIds: List<UUID>? = null,
    var fromDate: LocalDate? = null,
    var toDate: LocalDate? = null,
    var categoryIds: List<Long>? = null,
) : ExportRequestParams

data class LendingsPerContactExportParams(
    @field:NotNull var contactId: UUID? = null,
) : ExportRequestParams

data class LendingsAllExportParams(
    var includeSettled: Boolean? = null,
) : ExportRequestParams

data class AccountsSummaryExportParams(
    var asOfDate: LocalDate? = null,
) : ExportRequestParams

data class PostProcessingRequest(
    @field:NotNull var type: PostProcessingType? = null,
    var config: Map<String, Any?>? = null,
)
