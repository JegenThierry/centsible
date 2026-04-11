package beer.thierry.budgetplannerrest.model.category

data class CategoryDTO(
    val id: Long?,
    val name: String?,
    val icon: String?,
    val type: CategoryType?,
    val isSystem: Boolean = false
)
