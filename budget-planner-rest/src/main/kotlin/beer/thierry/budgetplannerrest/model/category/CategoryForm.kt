package beer.thierry.budgetplannerrest.model.category

data class CategoryForm(
    val name: String,
    val type: CategoryType,
    val icon: CategoryIcon,
)
