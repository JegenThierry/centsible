package beer.thierry.centsible.api.model.category

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CategoryForm(
    @field:NotBlank(message = "{validation.category.name.required}")
    @field:Size(max = 50, message = "{validation.category.name.tooLong}")
    var name: String = "",

    @field:NotBlank(message = "{validation.category.icon.required}")
    @field:Size(max = 50, message = "{validation.category.icon.tooLong}")
    @field:Pattern(
        regexp = "^i-lucide-[a-z0-9-]+$",
        message = "{validation.category.icon.pattern}"
    )
    var icon: String = "",

    @field:NotBlank(message = "{validation.category.color.required}")
    @field:Pattern(
        regexp = "^#[0-9a-fA-F]{6}$",
        message = "{validation.category.color.pattern}"
    )
    var color: String = "",

    @field:NotNull(message = "{validation.category.type.required}")
    var type: CategoryType = CategoryType.EXPENSE
)
