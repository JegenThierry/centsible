package beer.thierry.centsible.api.model.tag

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

/** A user-scoped, free-form label that can be attached to transactions (many-to-many). */
data class TagDTO(
    var id: Long? = null,
    var name: String = "",
    var color: String = "#6b7280",
    var createdAt: OffsetDateTime? = null,
    var modifiedAt: OffsetDateTime? = null,
)

data class TagForm(
    @field:NotBlank(message = "{validation.tag.name.required}")
    @field:Size(max = 50, message = "{validation.tag.name.tooLong}")
    var name: String = "",

    @field:Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "{validation.tag.color.invalid}")
    var color: String = "#6b7280",
)

/** Request body for replacing the full tag set of a transaction. */
data class TransactionTagsForm(
    var tagIds: List<Long> = emptyList(),
)
