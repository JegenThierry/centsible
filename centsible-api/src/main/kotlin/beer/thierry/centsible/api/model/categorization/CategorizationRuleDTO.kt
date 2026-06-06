package beer.thierry.centsible.api.model.categorization

import beer.thierry.centsible.api.model.category.CategoryDTO
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

data class CategorizationRuleDTO(
    val id: UUID,
    val matchType: MatchType,
    val pattern: String,
    val category: CategoryDTO,
    val priority: Int,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class CategorizationRuleForm(
    @field:NotNull
    var matchType: MatchType = MatchType.CONTAINS,

    @field:NotBlank
    @field:Size(min = 1, max = 255)
    var pattern: String = "",

    @field:NotNull
    @field:Positive
    var categoryId: Long = 0L,

    var priority: Int = 0,
)
