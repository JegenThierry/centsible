package beer.thierry.budgetplanner.api.model.category

data class CategoryDTO(
    var id: Long? = null,
    var name: String? = null,
    var icon: String? = null,
    var color: String? = null,
    var type: CategoryType? = null,
    var isSystem: Boolean = false
)
