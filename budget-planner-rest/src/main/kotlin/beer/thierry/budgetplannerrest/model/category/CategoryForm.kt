package beer.thierry.budgetplannerrest.model.category

data class CategoryForm(
    val name: String,
    val icon: String,
    val color: String,
    val type: CategoryType
)
