package beer.thierry.centsible.api.model.rule

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.tag.TagDTO
import java.math.BigDecimal
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

data class RuleConditionDTO(
    val id: UUID? = null,
    val field: RuleField,
    val operator: RuleOperator,
    val value: String,
)

data class RuleActionDTO(
    val id: UUID? = null,
    val type: RuleActionType,
    val category: CategoryDTO? = null,
    val tag: TagDTO? = null,
)

data class RuleDTO(
    val id: UUID,
    val name: String,
    val matchAll: Boolean,
    val enabled: Boolean,
    val priority: Int,
    val conditions: List<RuleConditionDTO>,
    val actions: List<RuleActionDTO>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class RuleConditionForm(
    @field:NotNull
    var field: RuleField = RuleField.DESCRIPTION,

    @field:NotNull
    var operator: RuleOperator = RuleOperator.CONTAINS,

    @field:NotBlank(message = "{validation.rule.condition.value.required}")
    @field:Size(max = 255, message = "{validation.rule.condition.value.tooLong}")
    var value: String = "",
)

data class RuleActionForm(
    @field:NotNull
    var type: RuleActionType = RuleActionType.SET_CATEGORY,

    var categoryId: Long? = null,
    var tagId: Long? = null,
)

data class RuleForm(
    @field:NotBlank(message = "{validation.rule.name.required}")
    @field:Size(max = 100, message = "{validation.rule.name.tooLong}")
    var name: String = "",

    var matchAll: Boolean = true,
    var enabled: Boolean = true,

    @field:Min(value = 0, message = "{validation.rule.priority.range}")
    @field:Max(value = 1000, message = "{validation.rule.priority.range}")
    var priority: Int = 0,

    @field:Valid
    @field:NotEmpty(message = "{validation.rule.conditions.required}")
    var conditions: List<RuleConditionForm> = emptyList(),

    @field:Valid
    @field:NotEmpty(message = "{validation.rule.actions.required}")
    var actions: List<RuleActionForm> = emptyList(),
)

data class RulePreviewMatch(
    val description: String,
    val amount: BigDecimal,
    val type: CategoryType,
)

data class RulePreviewResult(
    val matchedCount: Int,
    val sample: List<RulePreviewMatch>,
)
