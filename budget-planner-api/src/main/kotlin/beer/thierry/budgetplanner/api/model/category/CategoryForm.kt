package beer.thierry.budgetplanner.api.model.category

data class CategoryForm(
    var name: String = "",
    var icon: String = "",
    var color: String = "",
    var type: CategoryType = CategoryType.EXPENSE
)
