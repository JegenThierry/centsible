package beer.thierry.budgetplanner.api.model.category

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CategoryForm(
    @field:NotBlank(message = "Category name is required.")
    @field:Size(max = 50, message = "Category name must be at most 50 characters.")
    var name: String = "",

    @field:NotBlank(message = "Icon is required.")
    @field:Size(max = 50, message = "Icon must be at most 50 characters.")
    @field:Pattern(
        regexp = "^i-lucide-[a-z0-9-]+$",
        message = "Icon must be a Lucide name like 'i-lucide-tag'."
    )
    var icon: String = "",

    @field:NotBlank(message = "Color is required.")
    @field:Pattern(
        regexp = "^#[0-9a-fA-F]{6}$",
        message = "Color must be a hex value like '#3b82f6'."
    )
    var color: String = "",

    @field:NotNull(message = "Category type is required.")
    var type: CategoryType = CategoryType.EXPENSE
)
