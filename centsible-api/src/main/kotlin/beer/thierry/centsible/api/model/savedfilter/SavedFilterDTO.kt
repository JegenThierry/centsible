package beer.thierry.centsible.api.model.savedfilter

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class SavedFilterDTO(
    var id: Long? = null,
    var name: String = "",
    var filters: Map<String, Any?> = emptyMap(),
    var createdAt: OffsetDateTime? = null,
    var modifiedAt: OffsetDateTime? = null,
)

data class SavedFilterForm(
    @field:NotBlank(message = "{validation.savedFilter.name.required}")
    @field:Size(max = 100, message = "{validation.savedFilter.name.tooLong}")
    var name: String = "",

    @field:NotNull(message = "{validation.savedFilter.filters.required}")
    var filters: Map<String, Any?> = emptyMap(),
)
